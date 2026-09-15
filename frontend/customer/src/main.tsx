import { BrowserRouter, useNavigate, useLocation } from 'react-router-dom';
import { SessionProvider, useSession } from './shared/auth/SessionProvider';
import { usePageQuery, useDictionary, PageControls } from './shared/api/queries';
import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api, post, put, upload, loggedIn, label, Row } from './shared/api';
import { Login, Markdown, Attachment, logout } from './shared/ui';
import './shared/styles/style.css';
import { Practice, SelfGrade } from './features/learning/Practice';

function ApplicationForm({
  certificates,
  existing,
  onDone,
}: {
  certificates: Row[];
  existing?: Row;
  onDone: () => void;
}) {
  const [error, setError] = useState(''),
    [busy, setBusy] = useState(false);
  return (
    <form
      className="card form-grid"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError('');
        const f = new FormData(e.currentTarget);
        try {
          const data: Row = Object.fromEntries(f);
          const file = f.get('file') as File,
            aux = f.get('auxiliary') as File;
          delete data.file;
          delete data.auxiliary;
          data.attachmentId = file?.size ? (await upload(file)).id : existing?.attachmentId;
          data.auxiliaryId = aux?.size ? (await upload(aux)).id : existing?.auxiliaryId;
          if (existing) await put(`/certification/applications/${existing.id}`, data);
          else await post('/certification/applications', data);
          onDone();
        } catch (e) {
          setError((e as Error).message);
        } finally {
          setBusy(false);
        }
      }}
    >
      <h2>{existing ? '补充获证申请' : '申报已获证书'}</h2>
      <p className="notice">
        图片和自动识别仅作辅助。管理员将核对官方记录与账号归属，审核通过后才更新等级。请勿提供官方查询网站的密码。
      </p>
      <label>
        证书
        <select name="certificateId" defaultValue={existing?.certificateId} required>
          {certificates.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        持证人姓名
        <input name="holderName" defaultValue={existing?.holderName} required maxLength={100} />
      </label>
      <label>
        编号类型
        <select name="numberType" defaultValue={existing?.numberType || 'CERTIFICATE'}>
          <option value="CERTIFICATE">证书编号</option>
          <option value="MANAGEMENT">管理号</option>
          <option value="QUERY">查询编号</option>
        </select>
      </label>
      <label>
        编号
        <input name="number" defaultValue={existing?.number} required maxLength={100} />
      </label>
      <label>
        取得日期
        <input
          name="obtainedOn"
          type="date"
          defaultValue={existing?.obtainedOn}
          required
          max={new Date().toISOString().slice(0, 10)}
        />
      </label>
      <label>
        证书 PDF 或清晰图片
        <input
          name="file"
          type="file"
          accept="application/pdf,image/png,image/jpeg,image/webp"
          required={!existing}
        />
      </label>
      <label>
        辅助证明（可选）
        <input
          name="auxiliary"
          type="file"
          accept="application/pdf,image/png,image/jpeg,image/webp"
        />
      </label>
      <label>
        特殊情况说明
        <textarea name="note" defaultValue={existing?.note} />
      </label>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      <button disabled={busy}>{busy ? '正在提交…' : '提交申请'}</button>
    </form>
  );
}
function Certification({ certificates, refresh }: { certificates: Row[]; refresh: () => void }) {
  const { data, error, load, pagination } = usePageQuery('/certification/applications');
  const [show, setShow] = useState(false),
    [detail, setDetail] = useState<Row | null>(null),
    [editing, setEditing] = useState<Row | undefined>();
  return (
    <>
      <div className="section-title">
        <h2>我的获证认证</h2>
        <button
          onClick={() => {
            setEditing(undefined);
            setShow(!show);
          }}
        >
          ＋ 申报证书
        </button>
      </div>
      {error && <p className="error">{error}</p>}
      {show && (
        <ApplicationForm
          key={editing?.id || 'new'}
          certificates={certificates}
          existing={editing}
          onDone={() => {
            setShow(false);
            void load();
            refresh();
          }}
        />
      )}
      {!data.length && !show && (
        <div className="empty">尚无获证申请。已取得软考证书后，可在这里申请平台认证。</div>
      )}
      {data.map((x) => (
        <article className="card" key={x.id}>
          <div className="section-title">
            <h3>{x.certificateName}</h3>
            <span className="badge">{label(x.status)}</span>
          </div>
          <p>
            {x.holderName} · {x.number}
          </p>
          <p className="muted">
            预检查：{label(x.autoResult)}　官方查验：{label(x.officialResult)}
          </p>
          <button
            className="text-button"
            onClick={() =>
              api(`/certification/applications/${x.id}`)
                .then(setDetail)
                .catch((e) => alert(e.message))
            }
          >
            查看进度与审核意见
          </button>
        </article>
      ))}
      {detail && (
        <div className="card">
          <button className="text-button" onClick={() => setDetail(null)}>
            收起详情
          </button>
          <h3>{detail.certificateName}</h3>
          <p>
            {detail.holderName} · {detail.number}
          </p>
          <p>{detail.autoDetails.notice}</p>
          <Attachment id={detail.attachmentId} />
          {detail.reviews.map((r: Row) => (
            <p key={r.id}>
              {label(r.action)}：{r.reason} <small>{new Date(r.createdAt).toLocaleString()}</small>
            </p>
          ))}
          {detail.status === 'SUPPLEMENT' && (
            <button
              onClick={() => {
                setEditing(detail);
                setShow(true);
                setDetail(null);
              }}
            >
              补充并重新提交
            </button>
          )}
        </div>
      )}
      <PageControls pagination={pagination} />
    </>
  );
}
function Exam({ id, onClose }: { id: string; onClose: () => void }) {
  const [attempt, setAttempt] = useState<Row | null>(null),
    [answers, setAnswers] = useState<Row>({}),
    [message, setMessage] = useState(''),
    [busy, setBusy] = useState(false),
    [now, setNow] = useState(Date.now());
  const current = useRef<Row>({}),
    queue = useRef(Promise.resolve()),
    timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined),
    offset = useRef(0);
  useEffect(() => {
    api(`/exams/${id}`)
      .then((x) => {
        setAttempt(x);
        setAnswers(x.answers);
        current.current = x.answers;
        offset.current = Date.parse(x.serverTime) - Date.now();
      })
      .catch((e) => setMessage(e.message));
    const t = setInterval(() => setNow(Date.now() + offset.current), 1000);
    return () => {
      clearInterval(t);
      clearTimeout(timer.current);
    };
  }, [id]);
  const save = (value: Row) => {
    setMessage('保存中…');
    const task = queue.current
      .catch(() => {})
      .then(() => put(`/exams/${id}/answers`, value))
      .then(() => {
        setMessage('答案已保存');
      });
    queue.current = task;
    task.catch((e) => setMessage(`保存失败：${e.message}，请重试`));
    return task;
  };
  const change = (key: string, value: unknown) => {
    const next = { ...current.current, [key]: value };
    current.current = next;
    setAnswers(next);
    clearTimeout(timer.current);
    setMessage('有未保存的修改');
    timer.current = setTimeout(() => void save(next), 600);
  };
  const submit = async () => {
    if (!attempt) return;
    setBusy(true);
    clearTimeout(timer.current);
    try {
      await queue.current.catch(() => {});
      if (Date.parse(attempt.deadline) > Date.now() + offset.current) await save(current.current);
      setAttempt(await post(`/exams/${id}/submit`));
    } catch (e) {
      setMessage((e as Error).message);
    } finally {
      setBusy(false);
    }
  };
  if (!attempt) return <p>{message || '加载试卷…'}</p>;
  const done = attempt.status === 'SUBMITTED',
    remaining = Math.max(0, Math.ceil((Date.parse(attempt.deadline) - now) / 1000));
  return (
    <>
      <div className="exam-bar">
        <button
          className="text-button"
          onClick={async () => {
            if (!done && remaining > 0) {
              try {
                clearTimeout(timer.current);
                await save(current.current);
              } catch {
                return;
              }
            }
            onClose();
          }}
        >
          返回
        </button>
        <b>{attempt.snapshot.title}</b>
        <span>
          {done
            ? '已交卷'
            : `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}`}
        </span>
      </div>
      <p aria-live="polite">{message}</p>
      {!done && remaining === 0 && (
        <p className="notice">考试已到时，请交卷。以服务器已保存答案计分。</p>
      )}
      {done && (
        <section className="hero">
          <h2>
            {attempt.result.score} / {attempt.result.total} 分
          </h2>
          <p>
            用时 {Math.round(attempt.result.elapsedSeconds / 60)} 分钟
            {attempt.result.pendingManual ? ' · 主观题待评分' : ''}
          </p>
        </section>
      )}
      {attempt.snapshot.questions.map((q: Row, i: number) => (
        <article className="card" key={q.id}>
          <p className="muted">
            第 {i + 1} 题 · {q.points} 分
          </p>
          <Markdown text={q.stem} />
          {q.attachmentIds?.map((aid: string) => <Attachment key={aid} id={aid} />)}
          {q.options ? (
            <div className="options">
              {q.options.map((o: Row) => (
                <label key={o.id}>
                  <input
                    type={q.type === 'MULTIPLE' ? 'checkbox' : 'radio'}
                    name={q.id}
                    disabled={done || remaining === 0}
                    checked={(answers[q.id] || []).includes(o.id)}
                    onChange={(e) =>
                      change(
                        q.id,
                        q.type === 'MULTIPLE'
                          ? e.target.checked
                            ? [...(answers[q.id] || []), o.id]
                            : (answers[q.id] || []).filter((x: string) => x !== o.id)
                          : [o.id],
                      )
                    }
                  />
                  {o.text}
                </label>
              ))}
            </div>
          ) : q.type === 'BOOLEAN' ? (
            <select
              disabled={done || remaining === 0}
              value={answers[q.id]?.[0] || ''}
              onChange={(e) => change(q.id, [e.target.value])}
            >
              <option value="">请选择</option>
              <option value="true">正确</option>
              <option value="false">错误</option>
            </select>
          ) : (
            <>
              <>
                {q.children?.map((c: Row, n: number) => (
                  <Markdown key={n} text={`**子题 ${n + 1}**\n${c.stem}`} />
                ))}
              </>
              <textarea
                aria-label="作答"
                disabled={done || remaining === 0}
                placeholder={
                  q.type === 'FILL' ? '多个空用换行分隔' : '填写答案；案例题请标注子题序号'
                }
                value={q.type === 'FILL' ? (answers[q.id] || []).join('\n') : answers[q.id] || ''}
                onChange={(e) =>
                  change(q.id, q.type === 'FILL' ? e.target.value.split('\n') : e.target.value)
                }
              />
            </>
          )}
          {done && (
            <div className="answer">
              <p>
                参考答案：
                {JSON.stringify(
                  q.answer || q.rubric || q.children?.map((c: Row) => c.answer || c.rubric),
                )}
              </p>
              <Markdown text={q.explanation || ''} />
            </div>
          )}
        </article>
      ))}
      {done && attempt.result.pendingManual && <SelfGrade attempt={attempt} onDone={setAttempt} />}{' '}
      {done && attempt.result.subjectiveAssessment === 'SELF' && <p>主观题成绩来自用户自评。</p>}
      {!done && (
        <div className="actions">
          <button disabled={busy || remaining === 0} onClick={() => void save(current.current)}>
            立即保存
          </button>
          <button disabled={busy} onClick={() => void submit()}>
            {busy ? '提交中…' : '交卷并查看报告'}
          </button>
        </div>
      )}
    </>
  );
}
function Study() {
  const [kind, setKind] = useState('SYLLABUS'),
    [filter, setFilter] = useState('');
  const { data, error, pagination } = usePageQuery(`/content?kind=${kind}`);
  const [attempt, setAttempt] = useState<string | null>(null);
  const exams = usePageQuery('/exams', 'exam');
  const learning = usePageQuery('/learning', 'learning');
  if (attempt)
    return (
      <Exam
        id={attempt}
        onClose={() => {
          setAttempt(null);
          void exams.load();
          void learning.load();
        }}
      />
    );
  return (
    <>
      <Practice onStart={setAttempt} />
      <div className="tabs">
        {[
          ['SYLLABUS', '大纲'],
          ['KNOWLEDGE', '知识点'],
          ['MATERIAL', '资料'],
          ['PAPER', '模拟考试'],
        ].map(([k, t]) => (
          <button key={k} className={kind === k ? 'active' : ''} onClick={() => setKind(k)}>
            {t}
          </button>
        ))}
      </div>
      <input
        aria-label="筛选内容"
        placeholder="搜索标题"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />
      {error && <p className="error">{error}</p>}
      {!data.length && <div className="empty">暂无已发布内容，请等待管理员导入并审核。</div>}
      {data
        .filter((x) => x.title.includes(filter))
        .map((x) => (
          <article className="card" key={x.id}>
            <h3>{x.title}</h3>
            <Markdown text={x.payload.body || x.payload.description || ''} />
            {x.payload.subjects?.map((s: Row) => (
              <div key={s.code}>
                <h4>{s.name}</h4>
                {s.chapters?.map((c: Row) => <p key={c.code}>{c.name}</p>)}
              </div>
            ))}
            {x.payload.attachmentIds?.map((id: string) => <Attachment id={id} key={id} />)}
            {kind === 'PAPER' ? (
              <button
                onClick={() =>
                  post(`/exams/start/${x.id}`)
                    .then((a) => setAttempt(a.id))
                    .catch((e) => alert(e.message))
                }
              >
                开始考试
              </button>
            ) : (
              <button
                className="text-button"
                onClick={() =>
                  put(`/learning/${x.id}`, { favorite: true, progress: 100 })
                    .then(() => learning.load())
                    .catch((e) => alert(e.message))
                }
              >
                收藏并标记已学习
              </button>
            )}
          </article>
        ))}
      <PageControls pagination={pagination} />
      <h3>考试记录</h3>
      {exams.data.map((x) => (
        <button className="record" key={x.id} onClick={() => setAttempt(x.id)}>
          {new Date(x.startedAt).toLocaleString()} · {label(x.status)}
        </button>
      ))}
      <PageControls pagination={exams.pagination} />
      <h3>错题与收藏</h3>
      {learning.data
        .filter((x) => x.wrong || x.favorite)
        .map((x) => (
          <p key={x.entityId}>
            {x.title} {x.wrong ? '· 错题' : ''} {x.favorite ? '· 已收藏' : ''}
          </p>
        ))}
    </>
  );
}
function App() {
  const { token } = useSession();
  const signed = !!token;
  const [me, setMe] = useState<Row | null>(null),
    [certificates, setCertificates] = useState<Row[]>([]),
    [tab, setTab] = useState('home'),
    [error, setError] = useState('');
  const refresh = () =>
    api('/customer/me')
      .then(setMe)
      .catch((e) => setError(e.message));
  useEffect(() => {
    if (signed) {
      void refresh();
      api('/public/certificates')
        .then(setCertificates)
        .catch((e) => setError(e.message));
    }
  }, [signed]);
  if (!signed) return <Login onLogin={() => {}} />;
  return (
    <>
      <header>
        <a className="brand" href="/">
          知阶 <small>软考学习</small>
        </a>
        <button className="text-button" onClick={() => logout().catch((e) => setError(e.message))}>
          退出
        </button>
      </header>
      <main className="mobile-main">
        {error && <p className="error">{error}</p>}
        {tab === 'home' && (
          <>
            <section className="hero">
              <span>你好，{me?.username}</span>
              <h1>
                让每一次学习
                <br />
                更接近目标。
              </h1>
              <p>{me?.rules?.find((r: Row) => r.level === me.memberLevel)?.name || '普通会员'}</p>
              <div>
                {me?.badges?.map((b: Row, i: number) => (
                  <span className="badge" key={i}>
                    {b.name} · 平台审核通过
                  </span>
                ))}
              </div>
            </section>
            <div className="section-title">
              <h2>当前备考</h2>
              <span className="muted">与已获证书独立维护</span>
            </div>
            {me?.targets?.map((c: Row) => (
              <article className="card section-title" key={c.id}>
                <b>{c.name}</b>
                <button
                  className="text-button"
                  onClick={() =>
                    api(`/me/targets/${c.id}`, { method: 'DELETE' })
                      .then(refresh)
                      .catch((e) => setError(e.message))
                  }
                >
                  移除
                </button>
              </article>
            ))}
            <div className="card">
              <label>
                添加备考目标
                <select
                  defaultValue=""
                  onChange={(e) => {
                    if (e.target.value)
                      post(`/me/targets/${e.target.value}`)
                        .then(refresh)
                        .catch((e) => setError(e.message));
                    e.target.value = '';
                  }}
                >
                  <option value="">选择证书</option>
                  {certificates.map((c) => (
                    <option key={c.id} value={c.id}>
                      {['', '初级', '中级', '高级'][c.level]} · {c.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="card">
              <h3>今日，从一个知识点开始</h3>
              <p>按大纲学习，练习巩固，再用模拟考试检验。</p>
              <button onClick={() => setTab('study')}>进入学习中心 →</button>
            </div>
          </>
        )}
        {tab === 'study' && <Study />}
        {tab === 'certification' && (
          <Certification certificates={certificates} refresh={() => void refresh()} />
        )}
      </main>
      <nav className="bottom-nav">
        {[
          ['home', '我的进阶'],
          ['study', '学习考试'],
          ['certification', '获证认证'],
        ].map(([k, t]) => (
          <button key={k} className={tab === k ? 'active' : ''} onClick={() => setTab(k)}>
            {t}
          </button>
        ))}
      </nav>
    </>
  );
}
createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <SessionProvider>
        <App />
      </SessionProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
