import './style.css';
import {initializeAnalytics,event} from './analytics';
import {duration} from './form';
import {backend,loadForm,submit} from './backend';
import {message as showMessage, render as showForm} from './view';

const root=document.querySelector<HTMLElement>('#app')!;
const match=location.pathname.match(/^\/f\/([A-Za-z0-9_-]{1,128})\/?$/);
function message(text:string,retry?:()=>void,state:'info'|'loading'|'error'|'success'='info'){showMessage(root,text,retry,state);}
const started=Date.now();
async function run(){
 if(!match){message('Abre el enlace de un formulario para completar una solicitud.');return;}
 const id=match[1];
 message('Cargando formulario…', undefined, 'loading');
 try {
  const {db,app}=await backend();
  void initializeAnalytics(app);
  const key=`formlink-attempt:${id}`;
  let attempt:{id:string;start:number;complete?:boolean;openedSent?:boolean;submittedSent?:boolean};
  try {
   const previous=JSON.parse(sessionStorage.getItem(key)||'null');
   attempt=previous&&typeof previous.id==='string'&&/^[0-9a-f-]{36}$/.test(previous.id)&&Number.isFinite(previous.start)?previous:{id:crypto.randomUUID(),start:started};
  }catch{attempt={id:crypto.randomUUID(),start:started};}
  function remember(){try{sessionStorage.setItem(key,JSON.stringify(attempt));}catch{/* Storage can be unavailable; keep the current attempt in memory. */}}
  remember();
  if(attempt.complete){message('Tu solicitud ya fue enviada. Gracias.', undefined, 'success');return;}
  const form=await loadForm(db,id);
  const metadata={form_id:id,attempt_id:attempt.id,form_type:form.templateType};
  // Analytics readiness may lag behind the form; logging remains independent from submission.
  await initializeAnalytics(app);
  if(!attempt.openedSent){attempt.openedSent=true;remember();event('form_opened',metadata);}
  showForm(root,form,async answers=>{
   await submit(db,id,attempt.id,form,answers);
   attempt.complete=true;
   const seconds=duration(attempt.start,Date.now());
   if(!attempt.submittedSent&&seconds!==null){attempt.submittedSent=true;remember();event('form_submitted',{...metadata,duration_seconds:seconds,field_count:form.fields.length});}
   remember();
   message('Tu solicitud se guardó correctamente. Gracias.', undefined, 'success');
  });
 }catch(error){const code=(error as {code?:string}).code;message(code==='permission-denied'||(error as Error).message==='UNAVAILABLE'?'Este formulario no existe o ya no recibe solicitudes.':'No se pudo cargar el formulario. Comprueba tu conexión.',()=>{void run();},'error');}
}
void run();
