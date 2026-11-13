import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { before, after, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocFromServer, getDocs, collection, query, where, deleteDoc, Timestamp, writeBatch, serverTimestamp, updateDoc, collectionGroup } from 'firebase/firestore';

let environment;
before(async () => {
  // Rule fixtures are cleared between tests; keep them out of the app's demo data.
  environment = await initializeTestEnvironment({ projectId: 'demo-formlink-rules',
    firestore: { host: '127.0.0.1', port: 8080, rules: await readFile('../firestore.rules', 'utf8') } });
});
beforeEach(async () => { await environment.clearFirestore(); });
after(async () => { await environment.cleanup(); });
const draft = ownerId => ({ ownerId, title: 'Pedido real', status: 'DRAFT', fields: [],
  createdAt: Timestamp.now(), updatedAt: Timestamp.now(), publishedAt: null, version: 1 });

test('an owner creates, reads, lists and tombstones their forms without physically deleting them', async () => {
  const db = environment.authenticatedContext('owner').firestore();
  const ref = doc(db, 'forms', 'f');
  await assertSucceeds(setDoc(ref, draft('owner')));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(getDocs(query(collection(db, 'forms'), where('ownerId', '==', 'owner'))));
  await assertFails(deleteDoc(ref));
  await assertSucceeds(updateDoc(ref, { deleted: true, status: 'ARCHIVED', updatedAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(ref, { deleted: true, status: 'ARCHIVED', updatedAt: serverTimestamp() }));
  await assertFails(deleteDoc(ref));
});

test('an unpublished draft deleted on device A cannot be recreated by a pending edit on device B', async () => {
  const deviceA = environment.authenticatedContext('owner', { device: 'a' }).firestore();
  const deviceB = environment.authenticatedContext('owner', { device: 'b' }).firestore();
  const refA = doc(deviceA, 'forms', 'unpublished');
  const refB = doc(deviceB, 'forms', 'unpublished');
  await setDoc(refA, draft('owner'));
  const pendingEdit = { ...(await getDocFromServer(refB)).data(), title: 'Old edit from device B', version: 2 };
  await assertSucceeds(updateDoc(refA, { deleted: true, status: 'ARCHIVED', updatedAt: serverTimestamp() }));
  await assertFails(setDoc(refB, pendingEdit));
  await assertFails(updateDoc(refB, { title: pendingEdit.title }));
  await assertFails(updateDoc(refB, { deleted: false, status: 'DRAFT' }));
  await assertFails(deleteDoc(refB));
  const removed = (await getDocFromServer(refA)).data();
  assert.equal(removed.deleted, true);
  assert.equal(removed.status, 'ARCHIVED');
  assert.equal(removed.title, 'Pedido real');
  assert.equal((await getDocFromServer(doc(deviceA, 'publishedForms', 'unpublished'))).exists(), false);
  await assertSucceeds(setDoc(doc(deviceB, 'forms', 'new-id'), draft('owner')));
});
test('unauthenticated and another account cannot read or modify a form', async () => {
  const owner = environment.authenticatedContext('owner').firestore();
  await setDoc(doc(owner, 'forms', 'f'), draft('owner'));
  for (const db of [environment.unauthenticatedContext().firestore(), environment.authenticatedContext('other').firestore()]) {
    await assertFails(getDoc(doc(db, 'forms', 'f')));
    await assertFails(setDoc(doc(db, 'forms', 'f'), draft('other')));
    await assertFails(deleteDoc(doc(db, 'forms', 'f')));
    await assertFails(getDocs(collection(db, 'forms')));
  }
});
test('ownership transfer, publication and device paths are rejected', async () => {
  const db = environment.authenticatedContext('owner').firestore();
  const ref = doc(db, 'forms', 'f');
  await setDoc(ref, draft('owner'));
  await assertFails(setDoc(ref, draft('other')));
  await assertFails(setDoc(ref, { ...draft('owner'), status: 'PUBLISHED' }));
  await assertFails(setDoc(ref, { ...draft('owner'), coverPath: 'covers/private.jpg' }));
  await assertFails(setDoc(doc(db, 'forms/f/responses/r'), { answer: 'not supported' }));
});
const config = {placeholder:'',allowMultipleAnswers:false,includeTime:false,disallowPastDates:false,allowDecimals:false,minimum:'1',maximum:'10',unit:'',options:[{id:'a',label:'A'},{id:'b',label:'B'}]};
const field = (id='q', type='SHORT_TEXT') => ({id,type,label:'Pregunta',description:'',required:true,position:0,optionIds:['a','b'],config:{...config,allowMultipleAnswers:type==='CHECKBOX_GROUP'}});
async function publish(fields=[field()],cover) {
 const db=environment.authenticatedContext('owner').firestore();
 await setDoc(doc(db,'forms/f'),{...draft('owner'),fields});
 const batch=writeBatch(db);
 batch.update(doc(db,'forms/f'),{status:'PUBLISHED',publishedAt:serverTimestamp(),publishedVersion:1});
 batch.set(doc(db,'publishedForms/f'),{ownerId:'owner',title:'Pedido real',fields,active:true,version:1,fingerprint:'test',templateType:'ORDER_FORM',...(cover?{cover}:{}),publishedAt:serverTimestamp(),firstPublishedAt:serverTimestamp()});
 await assertSucceeds(batch.commit());
 return (await getDoc(doc(db,'publishedForms/f'))).data();
}
function response(db,p,values,id='00000000-0000-4000-8000-000000000001') {
 const batch=writeBatch(db);
 for(const part of [0,1])batch.set(doc(db,`forms/f/responseChecks/${id}_${part}`),{attemptId:id,part,answers:p.fields.map((f,i)=>({fieldId:f.id,value:values[i]}))});
 batch.set(doc(db,`forms/f/responses/${id}`),{ownerId:'owner',formId:'f',attemptId:id,title:p.title,fields:p.fields,version:p.version,submittedAt:serverTimestamp(),status:'PENDING',answers:p.fields.map((f,i)=>({fieldId:f.id,value:values[i]}))});
 batch.set(doc(db,`publishedForms/f/receipts/${id}`),{responseId:id,formId:'f',version:p.version,submittedAt:serverTimestamp()});
 return batch.commit();
}
test('published response is immutable and idempotent; only its owner reads and changes status',async()=>{
 const p=await publish();const publicDb=environment.unauthenticatedContext().firestore();
 await assertSucceeds(getDoc(doc(publicDb,'publishedForms/f')));
 await assertFails(getDocs(collection(publicDb,'publishedForms')));
 await assertSucceeds(response(publicDb,p,['Ana']));
 await assertFails(response(publicDb,p,['duplicado']));
 await assertFails(getDoc(doc(publicDb,'forms/f/responses/00000000-0000-4000-8000-000000000001')));
 await assertSucceeds(getDoc(doc(publicDb,'publishedForms/f/receipts/00000000-0000-4000-8000-000000000001')));
 const owner=environment.authenticatedContext('owner').firestore();const ref=doc(owner,'forms/f/responses/00000000-0000-4000-8000-000000000001');
 await assertSucceeds(getDocs(query(collectionGroup(owner,'responses'),where('ownerId','==','owner'))));
 await assertSucceeds(updateDoc(ref,{status:'COMPLETED'}));await assertSucceeds(updateDoc(ref,{status:'PENDING'}));
 await assertFails(updateDoc(ref,{title:'Alterado'}));
 await assertFails(getDoc(doc(environment.authenticatedContext('other').firestore(),ref.path)));
});
test('ten answers validate all nine types within rule evaluation limits',async()=>{
 const types=['SHORT_TEXT','PARAGRAPH','NUMBER','EMAIL','CHECKBOX','CHECKBOX_GROUP','DROPDOWN','MULTIPLE_CHOICE','DATE_TIME','SHORT_TEXT'];
 const p=await publish(types.map((type,i)=>({...field(`q${i}`,type),position:i})));
 const db=environment.unauthenticatedContext().firestore();
 const values=['text','paragraph\nsecond line','5','client@example.com',true,['a','b'],['a'],['a'],{date:Timestamp.fromDate(new Date('2099-01-01')),minute:null},'last'];
 await assertSucceeds(response(db,p,values));
 for(const [index,bad] of [[0,''],[2,'11'],[2,'1.5'],[3,'invalid'],[4,false],[5,['unknown']],[6,['a','b']],[8,{date:null,minute:null}]]){
  const v=[...values];v[index]=bad;
  await assertFails(response(db,p,v,'00000000-0000-4000-8000-000000000002'));
 }
});
test('private edits stay private and archiving preserves previous submissions',async()=>{
 const p=await publish();const owner=environment.authenticatedContext('owner').firestore();const publicDb=environment.unauthenticatedContext().firestore();
 await response(publicDb,p,['Antes']);
 await updateDoc(doc(owner,'forms/f'),{title:'Edición privada'});
 assert.equal((await getDoc(doc(publicDb,'publishedForms/f'))).data().title,'Pedido real');
 const batch=writeBatch(owner);batch.update(doc(owner,'forms/f'),{status:'ARCHIVED'});batch.update(doc(owner,'publishedForms/f'),{active:false});await assertSucceeds(batch.commit());
 await assertFails(getDoc(doc(publicDb,'publishedForms/f')));
 await assertFails(response(publicDb,p,['Después'],'00000000-0000-4000-8000-000000000002'));
 await assertSucceeds(getDoc(doc(owner,'forms/f/responses/00000000-0000-4000-8000-000000000001')));
 await assertFails(deleteDoc(doc(owner,'forms/f')));
});
test('ten short-text questions can be submitted',async()=>{
 const p=await publish(Array.from({length:10},(_,i)=>({...field(`q${i}`),position:i})));
 await assertSucceeds(response(environment.unauthenticatedContext().firestore(),p,Array(10).fill('Respuesta')));
});
test('date-only submissions use a validated UTC midnight timestamp',async()=>{
 const p=await publish([field('q','DATE_TIME')]);
 await assertSucceeds(response(environment.unauthenticatedContext().firestore(),p,[{date:Timestamp.fromDate(new Date('2099-01-01')),minute:null}]));
});
test('ten bounded numeric fields and missing proof rejection',async()=>{
 const p=await publish(Array.from({length:10},(_,i)=>({...field(`q${i}`,'NUMBER'),position:i})));
 const db=environment.unauthenticatedContext().firestore();
 await assertSucceeds(response(db,p,Array(10).fill('5')));
 await assertFails(setDoc(doc(db,'forms/f/responses/00000000-0000-4000-8000-000000000002'),{ownerId:'owner',formId:'f',attemptId:'00000000-0000-4000-8000-000000000002',title:p.title,fields:p.fields,version:1,submittedAt:serverTimestamp(),status:'PENDING',answers:p.fields.map(f=>({fieldId:f.id,value:'5'}))}));
});

test('cover metadata is optional, owner-only and restricted to the configured Cloudinary cloud',async()=>{
 await publish();
 const db=environment.authenticatedContext('owner').firestore();
 const cover={url:'https://res.cloudinary.com/uxrkjzti/image/upload/v123/formlink/covers/fixture.jpg',publicId:'formlink/covers/fixture',hash:'a'.repeat(64)};
 function change(value,context=db){
  const batch=writeBatch(context);
  batch.update(doc(context,'publishedForms/f'),{cover:value});
  return batch.commit();
 }
 await assertSucceeds(change(cover));
 const publicDb=environment.unauthenticatedContext().firestore();
 assert.equal((await getDoc(doc(publicDb,'publishedForms/f'))).data().cover.url,cover.url);
 for(const value of [
  {...cover,url:cover.url.replace('https:','http:')},
  {...cover,url:cover.url.replace('uxrkjzti','another')},
  {...cover,url:cover.url.replace('res.cloudinary.com','evil.example')},
  {...cover,hash:'text'}, {...cover,publicId:'another'}, {...cover,extra:'unwanted'},
 ]) await assertFails(change(value));
 await assertFails(change(null,publicDb));
 await assertFails(change(null,environment.authenticatedContext('other').firestore()));
 await assertSucceeds(response(publicDb,(await getDoc(doc(publicDb,'publishedForms/f'))).data(),['Respuesta con portada']));
 await assertSucceeds(change(null));
 assert.equal((await getDoc(doc(publicDb,'publishedForms/f'))).data().cover,null);
});

test('ten questions with a cover stay within rule evaluation limits',async()=>{
 const cover={url:'https://res.cloudinary.com/uxrkjzti/image/upload/v123/fixture.jpg',publicId:'fixture',hash:'a'.repeat(64)};
 await publish(Array.from({length:10},(_,i)=>({...field(`q${i}`,'NUMBER'),position:i})),cover);
 const publicDb=environment.unauthenticatedContext().firestore();
 await assertSucceeds(response(publicDb,(await getDoc(doc(publicDb,'publishedForms/f'))).data(),Array(10).fill('5')));
});


test('deletion closes public access, preserves requests and receipts, and prevents revival', async () => {
 const p = await publish();
 const owner = environment.authenticatedContext('owner').firestore();
 const publicDb = environment.unauthenticatedContext().firestore();
 const id = '00000000-0000-4000-8000-000000000001';
 await response(publicDb, p, ['Conservar']);
 const remove = db => {
  const batch = writeBatch(db);
  batch.update(doc(db, 'forms/f'), { deleted: true, status: 'ARCHIVED', updatedAt: serverTimestamp() });
  batch.update(doc(db, 'publishedForms/f'), { active: false });
  return batch.commit();
 };
 await assertFails(remove(environment.authenticatedContext('other').firestore()));
 await assertFails(remove(publicDb));
 // The private tombstone and public closure must be committed together.
 await assertFails(updateDoc(doc(owner, 'forms/f'), { deleted: true, status: 'ARCHIVED' }));
 await assertSucceeds(remove(owner));
 await assertSucceeds(remove(owner));
 await assertFails(getDoc(doc(publicDb, 'publishedForms/f')));
 await assertFails(response(publicDb, p, ['Tarde'], '00000000-0000-4000-8000-000000000002'));
 assert.equal((await getDoc(doc(owner, `forms/f/responses/${id}`))).data().answers[0].value, 'Conservar');
 await assertSucceeds(updateDoc(doc(owner, `forms/f/responses/${id}`), { status: 'COMPLETED' }));
 await assertSucceeds(getDoc(doc(publicDb, `publishedForms/f/receipts/${id}`)));
 await assertFails(deleteDoc(doc(owner, `forms/f/responses/${id}`)));
 await assertFails(deleteDoc(doc(owner, 'forms/f')));
 await assertFails(setDoc(doc(owner, 'forms/f'), draft('owner')));
 await assertFails(updateDoc(doc(owner, 'forms/f'), { deleted: false }));
 await assertFails(updateDoc(doc(owner, 'forms/f'), { title: 'Stale edit' }));
 const revive = writeBatch(owner);
 revive.update(doc(owner, 'forms/f'), { status: 'PUBLISHED', publishedVersion: 1 });
 revive.update(doc(owner, 'publishedForms/f'), { active: true });
 await assertFails(revive.commit());
});
