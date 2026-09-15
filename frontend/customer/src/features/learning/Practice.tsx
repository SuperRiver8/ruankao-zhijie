import { useState } from 'react';
import { post, Row } from '../../shared/api';
export function Practice({ onStart }: { onStart: (id: string) => void }) {
  const [error, setError] = useState(''),
    [busy, setBusy] = useState(false);
  return (
    <details className="card">
      <summary>专项练习 / 错题重练</summary>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError('');
          const data: Row = Object.fromEntries(new FormData(e.currentTarget));
          data.count = Number(data.count);
          data.wrongOnly = data.wrongOnly === 'on';
          try {
            onStart((await post('/practice', data)).id);
          } catch (e) {
            setError((e as Error).message);
          } finally {
            setBusy(false);
          }
        }}
      >
        <label>
          题量
          <input name="count" type="number" min={1} max={100} defaultValue={5} />
        </label>
        <label>
          题型
          <select name="type">
            <option value="">全部</option>
            {[
              ['SINGLE', '单选'],
              ['MULTIPLE', '多选'],
              ['BOOLEAN', '判断'],
              ['FILL', '填空'],
              ['SHORT', '简答'],
              ['CASE', '案例'],
              ['ESSAY', '论文'],
            ].map(([v, t]) => (
              <option key={v} value={v}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label>
          知识点编码
          <input name="knowledgeCode" placeholder="可选" />
        </label>
        <label>
          科目
          <input name="subject" placeholder="可选" />
        </label>
        <label>
          章节
          <input name="chapter" placeholder="可选" />
        </label>
        <label>
          难度
          <select name="difficulty">
            <option value="">全部</option>
            {[1, 2, 3, 4, 5].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </label>
        <label>
          <input name="wrongOnly" type="checkbox" style={{ width: 'auto' }} /> 只练我的错题
        </label>
        {error && <p className="error">{error}</p>}
        <button disabled={busy}>开始练习</button>
      </form>
    </details>
  );
}
export function SelfGrade({ attempt, onDone }: { attempt: Row; onDone: (a: Row) => void }) {
  const [error, setError] = useState('');
  return (
    <form
      className="card"
      onSubmit={async (e) => {
        e.preventDefault();
        const p = Object.fromEntries(
          [...new FormData(e.currentTarget)].map(([k, v]) => [k, Number(v)]),
        );
        try {
          onDone(await post(`/exams/${attempt.id}/self-grade`, p));
        } catch (e) {
          setError((e as Error).message);
        }
      }}
    >
      <h3>主观题自评</h3>
      <p>请对照每题参考答案和评分要点打分。报告将明确标记“用户自评”。</p>
      {attempt.result.details
        .filter((x: Row) => x.pending)
        .map((x: Row) => {
          const q = attempt.snapshot.questions.find((q: Row) => q.id === x.id);
          return (
            <label key={x.id}>
              {q.title}（满分 {q.points}）
              <input name={x.id} required type="number" min={0} max={q.points} step="0.5" />
            </label>
          );
        })}
      {error && <p className="error">{error}</p>}
      <button>保存自评分数</button>
    </form>
  );
}
