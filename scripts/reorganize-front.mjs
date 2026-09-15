import fs from 'node:fs';
const root='frontend/src/';
for(const dir of ['apps/mobile','apps/admin','features/learning','shared/api','shared/ui','shared/styles','shared/auth','shared/types'])fs.mkdirSync(root+dir,{recursive:true});
const moves={'mobile.tsx':'apps/mobile/main.tsx','admin.tsx':'apps/admin/main.tsx','api.ts':'shared/api/index.ts','shared.tsx':'shared/ui/index.tsx','practice.tsx':'features/learning/Practice.tsx','style.css':'shared/styles/style.css'};
for(const [old,next] of Object.entries(moves)){
 let text=fs.readFileSync(root+old,'utf8');const prefix=next.startsWith('apps')?'../../':next.startsWith('features')?'../../':'../';
 text=text.replaceAll("from './api'",`from '${prefix}shared/api'`).replaceAll("from './shared'",`from '${prefix}shared/ui'`).replaceAll("import './style.css'",`import '${prefix}shared/styles/style.css'`).replaceAll("from './practice'","from '../../features/learning/Practice'");
 if(next.startsWith('shared/'))text=text.replaceAll('../shared/api','../api');
 fs.writeFileSync(root+next,text);fs.unlinkSync(root+old);
}
for(const app of ['mobile','admin']){let text=fs.readFileSync('frontend/'+app+'/index.html','utf8').replace('../src/'+app+'.tsx','../src/apps/'+app+'/main.tsx');fs.writeFileSync('frontend/'+app+'/index.html',text);}
