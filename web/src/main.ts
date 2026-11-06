import './style.css';
import {initializeAnalytics,event} from './analytics';
import {duration} from './form';
import {backend,loadForm,submit} from './backend';
import {validate,type Field,type Value,type PublishedForm} from './form';

const root=document.querySelector<HTMLElement>('#app')!;
const match=location.pathname.match(/^\/f\/([A-Za-z0-9_-]{1,128})\/?$/);
function node<K extends keyof HTMLElementTagNameMap>(tag:K,text?:string):HTMLElementTagNameMap[K]{const n=document.createElement(tag);if(text)n.textContent=text;return n;}
function message(text:string,retry?:()=>void){root.replaceChildren(node('h1','FormLink'),node('p',text));if(retry){const b=node('button','Reintentar');b.onclick=retry;root.append(b);}}
const started=Date.now();
async function run(){
 if(!match){message('Abre el enlace de un formulario para completar una solicitud.');return;}
 const id=match[1];
 root.replaceChildren(node('p','Cargando formulario…'));
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
  if(attempt.complete){message('Tu solicitud ya fue enviada. Gracias.');return;}
  const form=await loadForm(db,id);
  const metadata={form_id:id,attempt_id:attempt.id,form_type:form.templateType};
  // Analytics readiness may lag behind the form; logging remains independent from submission.
  await initializeAnalytics(app);
  if(!attempt.openedSent){attempt.openedSent=true;remember();event('form_opened',metadata);}
  render(form,async answers=>{
   await submit(db,id,attempt.id,form,answers);
   attempt.complete=true;
   const seconds=duration(attempt.start,Date.now());
   if(!attempt.submittedSent&&seconds!==null){attempt.submittedSent=true;remember();event('form_submitted',{...metadata,duration_seconds:seconds,field_count:form.fields.length});}
   remember();
   message('Tu solicitud se guardó correctamente. Gracias.');
  });
 }catch(error){const code=(error as {code?:string}).code;message(code==='permission-denied'||(error as Error).message==='UNAVAILABLE'?'Este formulario no existe o ya no recibe solicitudes.':'No se pudo cargar el formulario. Comprueba tu conexión.',()=>{void run();});}
}
function render(form:PublishedForm,send:(answers:{fieldId:string;value:Value}[])=>Promise<void>){
 const header=node('header');header.append(node('p','FormLink'),node('h1',form.title));
 const element=node('form');element.noValidate=true;
 const readers:{field:Field;read:()=>Value;error:HTMLElement;focus:HTMLElement}[]=[];
 for(const f of form.fields){
  const wrap=node('div');wrap.className='field';
  const label=node('label',f.label+(f.required?' *':''));label.htmlFor=f.id;wrap.append(label);
  if(f.description){const help=node('p',f.description);help.className='help';wrap.append(help);}
  let read:()=>Value;let focus:HTMLElement;
  if(['MULTIPLE_CHOICE','CHECKBOX_GROUP','DROPDOWN'].includes(f.type)){
   if(f.type==='DROPDOWN'){
    const select=node('select');select.id=f.id;select.multiple=f.config.allowMultipleAnswers;
    if(!select.multiple){const empty=node('option','Selecciona una opción');empty.value='';select.append(empty);}
    for(const option of f.config.options){const o=node('option',option.label);o.value=option.id;select.append(o);}
    wrap.append(select);read=()=>Array.from(select.selectedOptions).map(o=>o.value).filter(Boolean);focus=select;
   }else{
    const group=node('fieldset');const legend=node('legend',f.label+(f.required?' *':''));group.append(legend);label.remove();const inputs:HTMLInputElement[]=[];
    for(const option of f.config.options){const row=node('label');const input=node('input');input.type=f.config.allowMultipleAnswers?'checkbox':'radio';input.name=f.id;input.value=option.id;row.append(input,document.createTextNode(option.label));group.append(row);inputs.push(input);}
    wrap.append(group);read=()=>inputs.filter(i=>i.checked).map(i=>i.value);focus=inputs[0]||group;
   }
  }else if(f.type==='CHECKBOX'){
   const input=node('input');input.type='checkbox';input.id=f.id;wrap.append(input);read=()=>input.checked;focus=input;
  }else if(f.type==='DATE_TIME'){
   const input=node('input');input.type='date';input.id=f.id;wrap.append(input);
   const time=node('input');time.type='time';time.setAttribute('aria-label',`${f.label}: hora`);if(f.config.includeTime)wrap.append(time);
   read=()=>({date:input.value?new Date(`${input.value}T00:00:00Z`):null,minute:f.config.includeTime&&time.value?Number(time.value.slice(0,2))*60+Number(time.value.slice(3,5)):null});focus=input;
  }else{
   const input=f.type==='PARAGRAPH'?node('textarea'):node('input');input.id=f.id;input.placeholder=f.config.placeholder;
   input.maxLength=10000;if(input instanceof HTMLInputElement){input.type=f.type==='EMAIL'?'email':'text';if(f.type==='NUMBER')input.inputMode=f.config.allowDecimals?'decimal':'numeric';}
   wrap.append(input);read=()=>input.value.trim();focus=input;
  }
  const error=node('p');error.className='error';error.id=`${f.id}-error`;error.setAttribute('role','alert');focus.setAttribute('aria-describedby',error.id);wrap.append(error);
  readers.push({field:f,read,error,focus});element.append(wrap);
 }
 const status=node('p');status.setAttribute('role','status');
 const button=node('button','Enviar solicitud');button.type='submit';element.append(button,status);
 let busy=false;
 element.onsubmit=async e=>{e.preventDefault();if(busy)return;
  let invalid=false;for(const r of readers){const err=validate(r.field,r.read());r.error.textContent=err||'';r.focus.setAttribute('aria-invalid',String(!!err));if(err&&!invalid){r.focus.focus();invalid=true;}}
  if(invalid)return;
  busy=true;button.disabled=true;status.textContent='Guardando solicitud…';
  try{await send(readers.map(r=>({fieldId:r.field.id,value:r.read()})));}
  catch{status.textContent='No se confirmó el envío. Comprueba tu conexión y reintenta. Si el formulario cambió o se retiró, vuelve a abrir su enlace.';}
  finally{busy=false;button.disabled=false;}
 };
 const footer=node('footer','Tus respuestas se envían al propietario del formulario. Las métricas de uso no incluyen tus respuestas.');root.replaceChildren(header,element,footer);
}
void run();
