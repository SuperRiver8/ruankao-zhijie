import fs from 'node:fs';
const base='frontend/src/';
let api=fs.readFileSync(base+'shared/api/index.ts','utf8');const labels=api.slice(api.indexOf('export const labels'));
fs.writeFileSync(base+'shared/api/index.ts',`import {request,documentRequest,sessionSnapshot,updateSession,attachmentUrl} from './client';
export {attachmentUrl};
export type Row = Record<string, any>;
export const loggedIn=()=>!!sessionSnapshot();
export function setToken(value:Row){updateSession(value.accessToken?value as import('./client').SessionToken:null);}
export async function api(path:string,init:RequestInit={}):Promise<any>{
 if(path==='/admin/imports/schema'||path==='/admin/imports/dictionary')return documentRequest(path);
 return request(path,{method:init.method,data:typeof init.body==='string'?JSON.parse(init.body):init.body,headers:Object.fromEntries(new Headers(init.headers)),signal:init.signal||undefined});
}
export const post=(path:string,body:Row={})=>api(path,{method:'POST',body:JSON.stringify(body)});
export const put=(path:string,body:Row)=>api(path,{method:'PUT',body:JSON.stringify(body)});
export async function upload(file:File,access='PRIVATE'){const data=new FormData();data.append('file',file);data.append('access',access);return api('/attachments',{method:'POST',body:data});}
`+labels);
let shared=fs.readFileSync(base+'shared/ui/index.tsx','utf8');shared=shared.slice(0,shared.indexOf('export async function logout()'))+`export async function logout(){try {await post('/logout');} finally {setToken({});queryClient.clear();}}
`;shared=shared.replace("import ReactMarkdown", "import {queryClient} from '../auth/SessionProvider';\nimport ReactMarkdown");fs.writeFileSync(base+'shared/ui/index.tsx',shared);
for(const app of ['mobile','admin']){
 const path=base+'apps/'+app+'/main.tsx';let s=fs.readFileSync(path,'utf8');
 s=s.replace(', useLoad','').replaceAll('useLoad(', 'usePageQuery(').replace("usePageQuery('/public/certificates')","useDictionary('/public/certificates')");
 s=`import {BrowserRouter,useNavigate,useLocation} from 'react-router-dom';\nimport {SessionProvider,useSession} from '../../shared/auth/SessionProvider';\nimport {usePageQuery,useDictionary,PageControls} from '../../shared/api/queries';\n`+s;
 s=s.replace('  const [signed, setSigned] = useState(loggedIn()),','  const {token}=useSession();\n  const signed=!!token;\n  const [');
 // 移除旧会话状态声明中的多余方括号。
 s=s.replace('  const [\n    [me, setMe]', '  const [me, setMe]');
 s=s.replaceAll('onLogin={() => setSigned(true)}','onLogin={() => {}}');
 s=s.replace('<React.StrictMode>','<React.StrictMode><BrowserRouter><SessionProvider>').replace('</React.StrictMode>','</SessionProvider></BrowserRouter></React.StrictMode>');
 s=s.replace(/  useEffect\(\(\) => \{\s*const t = setInterval\(\(\) => void load\(\), 4000\);\s*return \(\) => clearInterval\(t\);\s*\}, \[\]\);/,'');
 if(app==='admin'){
   s=s.replaceAll('const { data, error, load } = usePageQuery','const { data, error, load, pagination, isLoading } = usePageQuery').replace('const { data, error } = usePageQuery','const { data, error, pagination, isLoading } = usePageQuery');
   // 每个分页业务组件中的 Table 使用服务端总数。
   const names=['Certifications','Content','Imports','Users','Audits'];for(const name of names){const start=s.indexOf('function '+name+'(');let end=s.indexOf('\nfunction ',start+1);if(end<0)end=s.length;let block=s.slice(start,end).replace('<Table','<Table pagination={pagination} loading={isLoading}');s=s.slice(0,start)+block+s.slice(end);}
 } else {
   s=s.replace('const { data, error, load } = usePageQuery','const { data, error, load, pagination } = usePageQuery').replace('const { data, error } = usePageQuery','const { data, error, pagination } = usePageQuery');
   s=s.replace("usePageQuery('/exams')","usePageQuery('/exams','exam')").replace("usePageQuery('/learning')","usePageQuery('/learning','learning')");
   s=s.replace('<h3>考试记录</h3>','<PageControls pagination={pagination}/><h3>考试记录</h3>').replace('<h3>错题与收藏</h3>','<PageControls pagination={exams.pagination}/><h3>错题与收藏</h3>');
   const start=s.indexOf('function Certification('),end=s.indexOf('function Exam(');let block=s.slice(start,end);const close=block.lastIndexOf('</>');block=block.slice(0,close)+'<PageControls pagination={pagination}/>'+block.slice(close);s=s.slice(0,start)+block+s.slice(end);
 }
 fs.writeFileSync(path,s);
}
