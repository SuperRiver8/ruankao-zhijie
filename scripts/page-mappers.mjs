import fs from 'node:fs';
const base='backend/src/main/';
const definitions=[
 ['User','users','UserEntity','created_at','id',''],
 ['Content','contents','ContentProjection','v.created_at','e.id','@Param("kind") String kind, @Param("published") boolean published,'],
 ['Certification','applications','ApplicationProjection','a.created_at','a.id','@Param("userId") UUID userId,'],
 ['Import','batches','ImportBatchEntity','created_at','id','@Param("id") UUID id,'],
 ['Exam','attempts','ExamEntity','started_at','id','@Param("id") UUID id,'],
 ['Learning','learning','LearningProjection','l.updated_at','l.entity_id','@Param("id") UUID id,'],
 ['Audit','audits','AuditEntity','created_at','id',''],
];
for(const [group,method,type,time,id,params] of definitions){
 const xmlFile=base+'resources/mappers/'+group+'Mapper.xml';let xml=fs.readFileSync(xmlFile,'utf8');
 const re=new RegExp('<select id="'+method+'" resultMap="([^"]+)">([^]*?)</select>');const match=xml.match(re);if(!match)throw Error(method);
 const raw=match[2].replace(/ORDER BY[^]*$/,'').trim();
 const ordering=`ORDER BY <choose><when test="query.sort == 'id'">${id}</when><otherwise>${time}</otherwise></choose> <choose><when test="query.direction == 'asc'">ASC</when><otherwise>DESC</otherwise></choose>, ${id} ASC`;
 xml=xml.replace('</mapper>',`<select id="${method}Page" resultMap="${match[1]}">${raw}\n${ordering} LIMIT #{query.size} OFFSET #{query.offset}</select>\n<select id="${method}Count" resultType="long">SELECT count(*) FROM (${raw}) page_count</select>\n</mapper>`);
 // 内部组卷及字典导出继续使用独立的完整查询。
 xml=xml.replace(match[0],match[0].replace(/ LIMIT (500|100)\b/,''));fs.writeFileSync(xmlFile,xml);
 const file=base+'java/cn/zhijie/dao/'+group+'Mapper.java';let source=fs.readFileSync(file,'utf8');
 if(!source.includes('import org.apache.ibatis.annotations.Param;'))source=source.replace('package cn.zhijie.dao;','package cn.zhijie.dao;\nimport org.apache.ibatis.annotations.Param;');
 source=source.replace(/}\s*$/,`List<${type}> ${method}Page(${params} @Param("query") PageQuery query);\nlong ${method}Count(${params} @Param("query") PageQuery query);\n}\n`);fs.writeFileSync(file,source);
}
