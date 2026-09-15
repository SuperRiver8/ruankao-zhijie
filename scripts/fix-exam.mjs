import fs from 'node:fs';
const p = 'backend/src/main/java/cn/zhijie/service/ExamService.java';
let s = fs.readFileSync(p, 'utf8');
s = s
  .replaceAll('List<Map<String, Object>> questions', 'List<ContentProjection> questions')
  .replace('Map<String, Object> paper', 'ContentProjection paper')
  .replaceAll('Map<String, Object> row', 'ExamEntity row')
  .replace('private Map<String, Object> owned', 'private ExamEntity owned');
let start = s.indexOf('        int count =', s.indexOf('public ExamResponse practice')),
  end = s.indexOf('\n    private ExamResponse create', start);
s =
  s.slice(0, start) +
  `        int count=filters.count()==null?10:filters.count();check(count>=1&&count<=100,"练习题量为 1–100");
        Set<UUID> wrong=new HashSet<>();if(Boolean.TRUE.equals(filters.wrongOnly()))for(var record:learningMapper.learning(a.id()))if(Boolean.TRUE.equals(record.getWrong()))wrong.add(record.getEntityId());
        List<ContentProjection> questions=new ArrayList<>(contentMapper.contents("QUESTION",true).stream().filter(q->{
            var payload=q.getPayload();if(Boolean.TRUE.equals(filters.wrongOnly())&&!wrong.contains(q.getId()))return false;
            if(!matches(payload,"certificateCode",filters.certificateCode())||!matches(payload,"type",filters.type())||!matches(payload,"difficulty",filters.difficulty())||!matches(payload,"subject",filters.subject())||!matches(payload,"chapter",filters.chapter()))return false;
            if(filters.knowledgeCode()!=null&&!filters.knowledgeCode().isBlank()){boolean found=false;for(var code:payload.path("knowledgeCodes"))if(code.asText().equals(filters.knowledgeCode()))found=true;if(!found)return false;}return true;
        }).toList());
        check(questions.size()>=count,"匹配题量不足，当前可用 "+questions.size()+" 题");Collections.shuffle(questions);
        var paper=new ContentProjection();paper.setVersion(0);paper.setTitle(Boolean.TRUE.equals(filters.wrongOnly())?"错题重练":"专项练习");
        return create(a,null,paper,new ArrayList<>(questions.subList(0,count)),JSON.createObjectNode().put("durationMinutes",60).put("points",1));
    }
    private boolean matches(JsonNode payload,String key,String expected){return expected==null||expected.isBlank()||payload.path(key).asText().equals(expected);}
` +
  s.slice(end);
s = s
  .replace('paper.get("version")', 'paper.getVersion()')
  .replace('paper.get("title")', 'paper.getTitle()');
start = s.indexOf('        var result = new LinkedHashMap<>(row);');
end = s.indexOf('\n    @Transactional', start);
s =
  s.slice(0, start) +
  `        JsonNode snapshot=row.getSnapshot().deepCopy();if(row.getStatus().equals("IN_PROGRESS"))for(var q:snapshot.path("questions"))ContentService.stripAnswers(q);
        row.setSnapshot(snapshot);return ResponseMapper.exam(row);
    }
` +
  s.slice(end);
s = s
  .replace(
    'validateAnswers(tree(row.getSnapshot()), answers);',
    'validateAnswers(row.getSnapshot(), answers);',
  )
  .replace('JSON.valueToTree(answers)', 'JSON.valueToTree(answers.answers())')
  .replace(
    'private void validateAnswers(JsonNode snap, Map<String, Object> answers)',
    'private void validateAnswers(JsonNode snap, AnswersRequest answers)',
  )
  .replace('answers.keySet()', 'answers.answers().keySet()');
s = s
  .replace('.orElse(Map.of())', '.orElseGet(LearningProjection::new)')
  .replace('prior.getOrDefault("favorite", false)', 'Boolean.TRUE.equals(prior.getFavorite())')
  .replace('prior.getOrDefault("wrong", false)', 'Boolean.TRUE.equals(prior.getWrong())');
s = s
  .replace('check(scores.get(qid) instanceof Number,', 'check(scores.scores().get(qid) != null,')
  .replace('((Number) scores.get(qid)).doubleValue()', 'scores.scores().get(qid)');
s = s.replaceAll('map(', 'document('); // 固定业务数据已经强类型；此处仅构建冻结题目与评分 JSON 文档。
fs.writeFileSync(p, s);
