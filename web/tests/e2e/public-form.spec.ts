import {test,expect} from '@playwright/test';
import {initializeApp,deleteApp} from 'firebase/app';
import {initializeAuth,inMemoryPersistence,connectAuthEmulator,createUserWithEmailAndPassword} from 'firebase/auth';
import {getFirestore,connectFirestoreEmulator,doc,setDoc,writeBatch,serverTimestamp,getDocs,collection} from 'firebase/firestore';

const id=`web-${crypto.randomUUID()}`;
let owner:string;
let app:ReturnType<typeof initializeApp>;
let db:ReturnType<typeof getFirestore>;
test.beforeAll(async()=>{
 app=initializeApp({projectId:'demo-formlink',apiKey:'demo-api-key',appId:'test-web'},id);
 const auth=initializeAuth(app,{persistence:inMemoryPersistence});connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});
 owner=(await createUserWithEmailAndPassword(auth,`${id}@example.com`,'password123')).user.uid;
 db=getFirestore(app);connectFirestoreEmulator(db,'127.0.0.1',8080);
 const fields=[{id:'name',type:'SHORT_TEXT',label:'Nombre del pedido',description:'Sin datos reales en esta prueba.',required:true,position:0,optionIds:[],config:{placeholder:'',allowMultipleAnswers:false,includeTime:false,disallowPastDates:false,allowDecimals:true,minimum:'',maximum:'',unit:'',options:[]}}];
 await setDoc(doc(db,'forms',id),{ownerId:owner,title:'Pedido web de prueba',fields,status:'DRAFT',createdAt:serverTimestamp(),updatedAt:serverTimestamp(),publishedAt:null,version:1});
 const batch=writeBatch(db);batch.update(doc(db,'forms',id),{status:'PUBLISHED',publishedVersion:1,publishedAt:serverTimestamp()});
 batch.set(doc(db,'publishedForms',id),{ownerId:owner,title:'Pedido web de prueba',fields,active:true,version:1,fingerprint:'fixture',templateType:'ORDER_FORM',publishedAt:serverTimestamp(),firstPublishedAt:serverTimestamp()});await batch.commit();
});
test.afterAll(async()=>{await deleteApp(app);});
test('public client validates, retries offline, sends once and survives reload',async({page,context})=>{
 await page.goto(`/f/${id}`);await expect(page.getByRole('heading',{name:'Pedido web de prueba'})).toBeVisible();
 await page.getByRole('button',{name:'Enviar solicitud'}).click();await expect(page.getByText('Completa este campo.')).toBeVisible();
 await page.getByLabel('Nombre del pedido').fill('Pedido de prueba');
 await context.setOffline(true);await page.getByRole('button',{name:'Enviar solicitud'}).click();await expect(page.getByRole('status')).toContainText('No se confirmó',{timeout:25000});
 await context.setOffline(false);await page.getByRole('button',{name:'Enviar solicitud'}).click();await expect(page.getByText('Tu solicitud se guardó correctamente. Gracias.')).toBeVisible({timeout:25000});
 await page.reload();await expect(page.getByText('Tu solicitud ya fue enviada. Gracias.')).toBeVisible();
 const responses=await getDocs(collection(db,'forms',id,'responses'));expect(responses.size).toBe(1);expect(responses.docs[0].data().answers[0].value).toBe('Pedido de prueba');
});
test('unknown links show a clear error',async({page})=>{
 await page.goto('/f/missing-form');await expect(page.getByText('Este formulario no existe o ya no recibe solicitudes.')).toBeVisible();
});
