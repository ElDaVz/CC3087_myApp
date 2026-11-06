import {execFileSync} from 'node:child_process';
import {readFileSync} from 'node:fs';
const files=execFileSync('git',['ls-files','-z'],{encoding:'utf8'}).split('\0').filter(Boolean);
const patterns=[/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/,/"type"\s*:\s*"service_account"/,/gh[pousr]_[A-Za-z0-9]{30,}/];
const failures=[];
for(const file of files){
 if(/(?:^|\/)\.env(?:\.|$)/.test(file)&&!file.endsWith('.env.example'))failures.push(`${file}: local environment tracked`);
 if(file==='app/google-services.json')failures.push(`${file}: local Firebase configuration tracked`);
 let bytes;try{bytes=readFileSync(file);}catch{continue;}
 if(bytes.length>2_000_000||bytes.includes(0))continue;
 const text=bytes.toString('utf8');
 if(patterns.some(pattern=>pattern.test(text)))failures.push(`${file}: possible private credential (value suppressed)`);
 if(file.endsWith('AndroidManifest.xml')&&/usesCleartextTraffic\s*=\s*["']true/.test(text))failures.push(`${file}: global cleartext traffic enabled`);
}
if(failures.length){process.stderr.write(failures.join('\n')+'\n');process.exitCode=1;}
else process.stdout.write(`Repository checks passed for ${files.length} tracked files; no matched private credentials or global cleartext settings.\n`);
