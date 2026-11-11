export type Field = { id:string; type:string; label:string; description:string; required:boolean; optionIds:string[];
 config:{ placeholder:string; allowMultipleAnswers:boolean; includeTime:boolean; disallowPastDates:boolean;
 allowDecimals:boolean; minimum:string; maximum:string; unit:string; options:{id:string;label:string}[] } };
export type PublishedForm = {ownerId:string; title:string; fields:Field[]; version:number; active:boolean; templateType:string; cover?:{url:string;publicId:string;hash:string}|null};
export type Value = string | boolean | string[] | {date:Date|null; minute:number|null};
export type Answer = {fieldId:string; value:Value};
export function validate(f:Field, value:Value, now=new Date()):string|null {
 const missing = value === '' || value === false || (Array.isArray(value)&&!value.length) ||
   (typeof value==='object'&&!Array.isArray(value)&&(value===null||value.date===null));
 if(missing) return f.required?'Completa este campo.':null;
 if(typeof value==='string') {
  if(value.length>10000) return 'Máximo 10,000 caracteres.';
  if(f.type==='EMAIL'&&!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))return 'Introduce un correo válido.';
  if(f.type==='NUMBER') {
   if(!/^[+-]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)$/.test(value)||!Number.isFinite(Number(value)))return 'Introduce un número válido.';
   const n=Number(value);
   if(!f.config.allowDecimals&&!Number.isInteger(n))return 'Introduce un número entero.';
   if(f.config.minimum!==''&&n<Number(f.config.minimum))return `Mínimo: ${f.config.minimum}.`;
   if(f.config.maximum!==''&&n>Number(f.config.maximum))return `Máximo: ${f.config.maximum}.`;
  }
 }
 if(Array.isArray(value)) {
  if(new Set(value).size!==value.length||value.some(id=>!f.optionIds.includes(id)))return 'Selecciona una opción válida.';
  if(!f.config.allowMultipleAnswers&&value.length>1)return 'Selecciona una sola opción.';
 }
 if(typeof value==='object'&&!Array.isArray(value)) {
  const d=value.date;
  if(!d||!Number.isFinite(d.getTime()))return 'Selecciona una fecha válida.';
  const today=Date.UTC(now.getUTCFullYear(),now.getUTCMonth(),now.getUTCDate());
  if(f.config.disallowPastDates&&d.getTime()<today)return 'No se permiten fechas pasadas.';
  if(f.config.includeTime&&(value.minute===null||value.minute<0||value.minute>=1440))return 'Selecciona una hora válida.';
 }
 return null;
}
export function duration(start:number,end:number):number|null {
 const seconds=(end-start)/1000;
 return Number.isFinite(seconds)&&seconds>=0?seconds:null;
}

// Only the configured Cloudinary image environment may supply a public banner.
export function coverImageUrl(cover: PublishedForm['cover']): string | null {
 if (!cover || typeof cover.url !== 'string' || typeof cover.publicId !== 'string' ||
   !/^[A-Za-z0-9_/-]{1,240}$/.test(cover.publicId) || !/^[a-f0-9]{64}$/.test(cover.hash)) return null;
 try {
  const url = new URL(cover.url);
  const prefix = '/uxrkjzti/image/upload/';
  const path = url.pathname.slice(prefix.length).replace(/^v[0-9]+\//, '');
  return url.protocol === 'https:' && url.host === 'res.cloudinary.com' && !url.username && !url.password &&
   !url.search && !url.hash && url.pathname.startsWith(prefix) &&
   ['jpg','jpeg','png','webp'].some(extension => path === `${cover.publicId}.${extension}`) ? cover.url : null;
 } catch { return null; }
}
