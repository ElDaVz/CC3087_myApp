import type {FirebaseApp} from 'firebase/app';
import {getAnalytics,isSupported,logEvent,setAnalyticsCollectionEnabled,type Analytics} from 'firebase/analytics';
let sdk:Analytics|undefined;
let environment='development';
export async function initializeAnalytics(app:FirebaseApp){
 if(import.meta.env.VITE_FIREBASE_EMULATORS==='true')return;
 const enabled=import.meta.env.VITE_TELEMETRY_ENABLED==='true'||(import.meta.env.PROD&&import.meta.env.VITE_TELEMETRY_ENABLED!=='false');
 if(!enabled||!app.options.measurementId)return;
 try{if(await isSupported()){
  sdk=getAnalytics(app);setAnalyticsCollectionEnabled(sdk,true);
  environment=import.meta.env.VITE_ANALYTICS_ENV||(location.hostname===`${app.options.projectId}.web.app`||location.hostname===`${app.options.projectId}.firebaseapp.com`?'users':'development');
 }}catch{/* Measurement must not prevent filling or sending a form. */}
}
const allowed=new Set(['form_id','attempt_id','form_type','duration_seconds','field_count']);
export function event(name:'form_opened'|'form_submitted',parameters:Record<string,string|number>){
 if(!sdk||Object.keys(parameters).some(k=>!allowed.has(k)))return;
 if(!/^draft-[0-9a-f-]{36}$/.test(String(parameters.form_id))||!/^[0-9a-f-]{36}$/.test(String(parameters.attempt_id)))return;
 if(!['CUSTOM','LEGACY','JOB_APPLICATION','ORDER_FORM','EVENT_RSVP','FEEDBACK'].includes(String(parameters.form_type)))return;
 if(parameters.duration_seconds!==undefined&&(!Number.isFinite(parameters.duration_seconds)||Number(parameters.duration_seconds)<0))return;
 try{logEvent(sdk,name,{...parameters,environment});}catch{/* Confirmed submissions stay successful if telemetry fails. */}
}
