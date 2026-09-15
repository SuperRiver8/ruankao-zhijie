import fs from 'node:fs';
const base='backend/src/main/java/cn/zhijie/';
const definitions=[
 ['UserAdministrationService','users','UserResponse','userMapper','users','','user','a'],
 ['ContentService','list','ContentResponse','contentMapper','contents','kind, !admin,','content','a'],
 ['MembershipService','list','ApplicationResponse','applications','applications','admin ? null : actor.id(),','application','actor'],
 ['ImportService','list','ImportBatchResponse','importMapper','batches','a.id(),','importBatch','a'],
 ['ExamService','list','ExamSummaryResponse','examMapper','attempts','a.id(),','examSummary','a'],
 ['LearningService','learning','LearningResponse','learningMapper','learning','a.id(),','learning','a'],
 ['AuditQueryService','audits','AuditResponse','auditMapper','audits','','audit','a'],
];
for(const [file,method,type,mapper,query,params,conversion] of definitions){
 const path=base+'service/'+file+'.java';let s=fs.readFileSync(path,'utf8');const re=new RegExp('public List<'+type+'> '+method+'\\(([^)]*)\\)');const match=s.match(re);if(!match)throw Error(file);
 const start=match.index,open=s.indexOf('{',start);let end=open+1,d=1;while(d){if(s[end]==='{')d++;if(s[end]==='}')d--;end++;}let block=s.slice(start,end);
 block=block.replace(match[0],`public PageResponse<${type}> ${method}(${match[1]}, PageQuery query)`);
 const ret=block.indexOf('return ');const finish=block.lastIndexOf(';');let expression=block.slice(ret+7,finish).replace(new RegExp(mapper+'\\s*\\.\\s*'+query+'\\([^]*?\\)\\s*\\.stream\\(\\)'),mapper+'.'+query+'Page('+params+' query).stream()');
 // 身份参数含有方法调用，按最后一个 stream 分界替换查询部分。
 const stream=expression.indexOf('.stream()');if(stream>=0)expression=mapper+'.'+query+'Page('+params+' query)'+expression.slice(stream);
 block=block.slice(0,ret)+`return PageResponse.of(${expression}, ${mapper}.${query}Count(${params} query), query)`+block.slice(finish);
 s=s.slice(0,start)+block+s.slice(end);fs.writeFileSync(path,s);
}
const controllers={UserAdministrationController:['users'],ContentController:['list','adminList'],MembershipController:['list','queue'],ImportController:['list'],ExamController:['list'],LearningController:['learning'],AuditController:['audits']};
for(const [file,names] of Object.entries(controllers)){
 const path=base+'controller/'+file+'.java';let s=fs.readFileSync(path,'utf8');if(!s.includes('import jakarta.validation.Valid;'))s=s.replace('package cn.zhijie.controller;','package cn.zhijie.controller;\nimport jakarta.validation.Valid;');s=s.replace('package cn.zhijie.controller;','package cn.zhijie.controller;\nimport cn.zhijie.pojo.query.PageQuery;');
 for(const name of names){const re=new RegExp('ApiResponse<List<(\\w+)>> '+name+'\\(([^]*?)\\) \\{');const m=s.match(re);if(!m)throw Error(file+name);const start=m.index,end=s.indexOf('\n    }',start);let block=s.slice(start,end);block=block.replace(m[0],`ApiResponse<PageResponse<${m[1]}>> ${name}(${m[2]}, @Valid @ModelAttribute PageQuery query) {`);block=block.replace(/(application|service)\.(\w+)\(([^]*?)\)/, '$1.$2($3, query)');s=s.slice(0,start)+block+s.slice(end);}
 fs.writeFileSync(path,s);
}
