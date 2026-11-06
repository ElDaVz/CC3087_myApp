import { readFile } from 'node:fs/promises';
import { before, after, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, query, where, deleteDoc, Timestamp } from 'firebase/firestore';

let environment;
before(async () => {
  environment = await initializeTestEnvironment({ projectId: 'demo-formlink',
    firestore: { host: '127.0.0.1', port: 8080, rules: await readFile('../firestore.rules', 'utf8') } });
});
beforeEach(async () => { await environment.clearFirestore(); });
after(async () => { await environment.cleanup(); });
const draft = ownerId => ({ ownerId, title: 'Pedido real', status: 'DRAFT', fields: [],
  createdAt: Timestamp.now(), updatedAt: Timestamp.now(), publishedAt: null, version: 1 });

test('an owner creates, reads, lists and deletes their forms', async () => {
  const db = environment.authenticatedContext('owner').firestore();
  const ref = doc(db, 'forms', 'f');
  await assertSucceeds(setDoc(ref, draft('owner')));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(getDocs(query(collection(db, 'forms'), where('ownerId', '==', 'owner'))));
  await assertSucceeds(deleteDoc(ref));
  await assertSucceeds(deleteDoc(ref));
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
