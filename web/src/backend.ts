import {initializeApp,type FirebaseOptions} from 'firebase/app';
import {getFirestore,connectFirestoreEmulator,doc,getDocFromServer,writeBatch,serverTimestamp,Timestamp,memoryLocalCache,initializeFirestore} from 'firebase/firestore';
import type {PublishedForm,Answer} from './form';

let instance:Promise<{app:ReturnType<typeof initializeApp>;db:ReturnType<typeof getFirestore>}>|undefined;
export function backend() {
 if(!instance)instance=createBackend().catch(error=>{instance=undefined;throw error;});
 return instance;
}
async function createBackend() {
 const emulator=import.meta.env.VITE_FIREBASE_EMULATORS==='true';
 const config:FirebaseOptions=emulator?{projectId:'demo-formlink',apiKey:'demo-api-key',appId:'1:1234567890:web:formlink'}:
  import.meta.env.VITE_FIREBASE_CONFIG?JSON.parse(import.meta.env.VITE_FIREBASE_CONFIG):await fetch('/__/firebase/init.json').then(r=>{if(!r.ok)throw Error('CONFIG');return r.json();});
 const app=initializeApp(config);
 const db=initializeFirestore(app,{localCache:memoryLocalCache()});
 if(emulator)connectFirestoreEmulator(db,'127.0.0.1',8080);
 return {app,db};
}
export async function loadForm(db:ReturnType<typeof getFirestore>,id:string):Promise<PublishedForm> {
 const snapshot=await withTimeout(getDocFromServer(doc(db,'publishedForms',id)));
 if(!snapshot.exists())throw Error('UNAVAILABLE');
 const form=snapshot.data() as PublishedForm;
 if(!form.active||!Array.isArray(form.fields)||form.fields.length===0||form.fields.length>10)throw Error('UNAVAILABLE');
 return form;
}
export async function submit(db:ReturnType<typeof getFirestore>,id:string,attempt:string,form:PublishedForm,answers:Answer[]) {
 const receipt=doc(db,'publishedForms',id,'receipts',attempt);
 if((await getDocFromServer(receipt)).exists())return;
 const batch=writeBatch(db);
 const wireAnswers=answers.map(a=>{const v=a.value;return {...a,value:typeof v==='object'&&!Array.isArray(v)?{...v,date:v.date?Timestamp.fromDate(v.date):null}:v};});
 for(const part of [0,1])batch.set(doc(db,'forms',id,'responseChecks',`${attempt}_${part}`),{attemptId:attempt,part,answers:wireAnswers});
 batch.set(doc(db,'forms',id,'responses',attempt),{ownerId:form.ownerId,formId:id,attemptId:attempt,title:form.title,
  fields:form.fields,version:form.version,submittedAt:serverTimestamp(),status:'PENDING',answers:wireAnswers});
 batch.set(receipt,{responseId:attempt,formId:id,version:form.version,submittedAt:serverTimestamp()});
 try {await withTimeout(batch.commit());}
 catch(error) {if(!(await getDocFromServer(receipt)).exists())throw error;}
}

export async function withTimeout<T>(promise:Promise<T>,millis=15000):Promise<T>{
 let timer:ReturnType<typeof setTimeout>|undefined;
 try{return await Promise.race([promise,new Promise<T>((_,reject)=>{timer=setTimeout(()=>reject(Error('TIMEOUT')),millis);})]);}
 finally{if(timer)clearTimeout(timer);}
}
