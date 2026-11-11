import {describe,it,expect} from 'vitest';
import {coverImageUrl,validate,duration,type Field} from '../src/form';
const field=(type:string,required=true):Field=>({id:'q',label:'Pregunta',description:'',type,required,optionIds:['a','b'],config:{placeholder:'',allowMultipleAnswers:false,includeTime:false,disallowPastDates:false,allowDecimals:false,minimum:'1',maximum:'10',unit:'',options:[{id:'a',label:'A'},{id:'b',label:'B'}]}});
describe('published answers',()=>{
 it('requires an answer without requiring optional fields',()=>{expect(validate(field('SHORT_TEXT'),'')).toBeTruthy();expect(validate(field('SHORT_TEXT',false),'')).toBeNull();});
 it('checks numeric range and integer constraint',()=>{expect(validate(field('NUMBER'),'1.5')).toBeTruthy();expect(validate(field('NUMBER'),'11')).toBeTruthy();expect(validate(field('NUMBER'),'1e2')).toBeTruthy();expect(validate(field('NUMBER'),'5')).toBeNull();});
 it('rejects invalid email and unknown or duplicate options',()=>{expect(validate(field('EMAIL'),'bad')).toBeTruthy();expect(validate(field('MULTIPLE_CHOICE'),['bad'])).toBeTruthy();expect(validate(field('CHECKBOX_GROUP'),['a','a'])).toBeTruthy();expect(validate(field('MULTIPLE_CHOICE'),['a'])).toBeNull();});
 it('rejects past dates and missing times',()=>{const f=field('DATE_TIME');f.config.disallowPastDates=true;f.config.includeTime=true;expect(validate(f,{date:new Date('2020-01-01'),minute:1},new Date('2026-01-01'))).toBeTruthy();expect(validate(f,{date:new Date('2026-01-02'),minute:null},new Date('2026-01-01'))).toBeTruthy();});
 it('excludes invalid durations',()=>{expect(duration(2000,1000)).toBeNull();expect(duration(0,NaN)).toBeNull();expect(duration(1000,2500)).toBe(1.5);});
});

describe('public cover URLs',()=>{
 const asset={url:'https://res.cloudinary.com/uxrkjzti/image/upload/v123/formlink/covers/random.jpg',publicId:'formlink/covers/random',hash:'a'.repeat(64)};
 it('keeps legacy forms without an image and accepts the configured cloud',()=>{
  expect(coverImageUrl(undefined)).toBeNull();expect(coverImageUrl(null)).toBeNull();expect(coverImageUrl(asset)).toBe(asset.url);
 });
 it('rejects unsafe URLs and mismatched metadata',()=>{
  for(const url of [asset.url.replace('https:','http:'),asset.url.replace('uxrkjzti','other'),asset.url.replace('res.cloudinary.com','res.cloudinary.com.evil.example'),asset.url+'?secret=x',asset.url.replace('.jpg','.svg'),asset.url.replace('/image/','/video/')])
   expect(coverImageUrl({...asset,url})).toBeNull();
  expect(coverImageUrl({...asset,publicId:'another'})).toBeNull();
 });
});
