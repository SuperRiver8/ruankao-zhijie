import { AdminAccounts } from './AdminAccounts';
import { AccountSettings } from './AccountSettings';
import { BrowserRouter, useNavigate, useLocation } from 'react-router-dom';
import { SessionProvider, useSession } from './shared/auth/SessionProvider';
import { usePageQuery, useDictionary, PageControls } from './shared/api/queries';
import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Layout,
  Menu,
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Checkbox,
  Alert,
  App as AntApp,
  ConfigProvider,
  InputNumber,
} from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { api, post, put, loggedIn, label, Row, upload } from './shared/api';
import { Login, Attachment, JsonView, logout } from './shared/ui';
import './shared/styles/style.css';
const kindOptions = ['KNOWLEDGE', 'SYLLABUS', 'MAPPING', 'QUESTION', 'MATERIAL', 'PAPER'].map(
  (value) => ({ value, label: label(value) }),
);
function Review({ record, onDone }: { record: Row; onDone: () => void }) {
  const [detail, setDetail] = useState<Row | null>(null);
  const { message } = AntApp.useApp();
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    api(`/certification/applications/${record.id}`)
      .then(setDetail)
      .catch((e) => message.error(e.message));
  }, [record.id]);
  if (!detail) return <p>正在加载申请…</p>;
  return (
    <>
      <Alert
        type="warning"
        showIcon
        message="证书真实与账号归属必须分别核实"
        description="图片、OCR 和同名不能作为归属证明。官方查询不可用或未收录时请选择“无法确认”。审核通过对外显示“平台审核通过”。"
      />
      <div className="review-columns">
        <section>
          <h3>证书原件</h3>
          <Attachment inline id={detail.attachmentId} />
          {detail.auxiliaryId && (
            <>
              <h4>辅助证明</h4>
              <Attachment inline id={detail.auxiliaryId} />
            </>
          )}
        </section>
        <section>
          <h3>用户申报</h3>
          <p>账号：{detail.userId}</p>
          <p>
            {detail.holderName} · {detail.certificateName}
          </p>
          <p>
            {detail.numberType}：{detail.number} · {detail.obtainedOn}
          </p>
          <p>{detail.note}</p>
          <Space>
            <Tag>{label(detail.autoResult)}</Tag>
            <Tag>{label(detail.officialResult)}</Tag>
            <Tag>{label(detail.status)}</Tag>
          </Space>
          <h4>自动预检查与差异</h4>
          <JsonView data={detail.autoDetails} />
          <h4>官方查验入口</h4>
          <p>
            <a href="https://www.ruankao.org.cn/" target="_blank" rel="noreferrer">
              中国计算机技术职业资格网
            </a>{' '}
            ·{' '}
            <a href="https://www.cpta.com.cn/" target="_blank" rel="noreferrer">
              中国人事考试网
            </a>
          </p>
          <Form
            layout="vertical"
            initialValues={{
              officialResult: 'UNCERTAIN',
              action: detail.status === 'APPROVED' ? 'REVOKED' : 'SUPPLEMENT',
            }}
            onFinish={async (p) => {
              setBusy(true);
              try {
                await post(`/admin/certifications/${detail.id}/review`, p);
                message.success('审核决定已保存');
                onDone();
              } catch (e) {
                message.error((e as Error).message);
              } finally {
                setBusy(false);
              }
            }}
          >
            <Form.Item name="officialResult" label="官方核验结果">
              <Select
                options={['UNVERIFIED', 'MATCH', 'MISMATCH', 'UNCERTAIN'].map((value) => ({
                  value,
                  label: label(value),
                }))}
              />
            </Form.Item>
            <Form.Item name="officialSource" label="实际查验渠道 / 查询结果引用">
              <Input />
            </Form.Item>
            <Form.Item name="evidence" label="官方查验依据（加密保存）">
              <Input.TextArea />
            </Form.Item>
            <Form.Item name="ownershipEvidence" label="账号归属依据（加密保存）">
              <Input.TextArea placeholder="记录核对的身份依据，不能只填写姓名相同" />
            </Form.Item>
            <Form.Item name="ownershipConfirmed" valuePropName="checked">
              <Checkbox>已核实持证人对应当前账号的已核实身份</Checkbox>
            </Form.Item>
            <Form.Item name="action" label="审核决定" rules={[{ required: true }]}>
              <Select
                options={(detail.status === 'APPROVED'
                  ? ['REVOKED']
                  : ['REVIEWING', 'SUPPLEMENT', 'APPROVED', 'REJECTED']
                ).map((value) => ({ value, label: label(value) }))}
              />
            </Form.Item>
            <Form.Item
              name="reason"
              label="审核理由（用户可见，请勿包含证件号码）"
              rules={[{ required: true }]}
            >
              <Input.TextArea />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={busy}>
              保存审核决定
            </Button>
          </Form>
          <h4>历史审核</h4>
          {detail.reviews.map((r: Row) => (
            <p key={r.id}>
              {label(r.action)} · {r.reason} · {new Date(r.createdAt).toLocaleString()}
            </p>
          ))}
          <h4>官方查验记录</h4>
          <JsonView data={detail.verifications} />
        </section>
      </div>
    </>
  );
}
function Certifications() {
  const { data, error, load, pagination, isLoading } = usePageQuery('/admin/certifications');
  const [selected, setSelected] = useState<Row | null>(null);
  return (
    <>
      <h2>获证认证审核</h2>
      {error && <Alert type="error" message={error} />}
      <Table
        pagination={pagination}
        loading={isLoading}
        rowKey="id"
        dataSource={data}
        columns={[
          { title: '申请人', dataIndex: 'username' },
          { title: '姓名', dataIndex: 'holderName' },
          { title: '证书', dataIndex: 'certificateName' },
          { title: '预检查', dataIndex: 'autoResult', render: label },
          { title: '官方结果', dataIndex: 'officialResult', render: label },
          { title: '状态', dataIndex: 'status', render: label },
          {
            title: '操作',
            render: (_, r) => <Button onClick={() => setSelected(r)}>查验与审核</Button>,
          },
        ]}
      />
      <Modal
        open={!!selected}
        onCancel={() => setSelected(null)}
        footer={null}
        width={1250}
        destroyOnClose
        title="证书查验与审核"
      >
        {selected && (
          <Review
            record={selected}
            onDone={() => {
              setSelected(null);
              void load();
            }}
          />
        )}
      </Modal>
    </>
  );
}
const example = {
  title: '进程与线程',
  type: 'SINGLE',
  stem: '下列哪一项是资源分配的基本单位？',
  difficulty: 2,
  certificateCode: 'SOFTWARE_DESIGNER',
  knowledgeCodes: [],
  options: [
    { id: 'A', text: '进程' },
    { id: 'B', text: '线程' },
  ],
  answer: ['A'],
  explanation: '进程是资源分配的基本单位。',
};
function Content() {
  const [kind, setKind] = useState('QUESTION');
  const { data, error, load, pagination, isLoading } = usePageQuery(`/admin/content?kind=${kind}`);
  const [selected, setSelected] = useState<Row | null>(null),
    [form] = Form.useForm();
  const { message } = AntApp.useApp();
  const edit = (r: Row) => {
    setSelected(r);
    form.setFieldsValue({
      kind: r.kind || kind,
      namespace: r.namespace || 'local',
      externalId: r.externalId || '',
      payload: JSON.stringify(
        r.payload || (kind === 'QUESTION' ? example : { title: '', body: '' }),
        null,
        2,
      ),
    });
  };
  return (
    <>
      <div className="section-title">
        <h2>内容与版本管理</h2>
        <Space>
          <Select
            value={kind}
            options={kindOptions}
            onChange={setKind}
            style={{ width: 'max-content', minWidth: 120 }}
            popupMatchSelectWidth={false}
          />
          <Button type="primary" onClick={() => edit({})}>
            新增内容
          </Button>
        </Space>
      </div>
      {error && <Alert type="error" message={error} />}
      <Table
        pagination={pagination}
        loading={isLoading}
        rowKey="id"
        dataSource={data}
        columns={[
          { title: '编码', dataIndex: 'externalId' },
          { title: '标题', dataIndex: 'title' },
          { title: '版本', dataIndex: 'version' },
          { title: '状态', dataIndex: 'status', render: label },
          {
            title: '操作',
            render: (_, r) => (
              <Space>
                <Button onClick={() => edit(r)}>编辑 / 新版本</Button>
                {r.status === 'PENDING' && (
                  <>
                    <Button
                      onClick={() =>
                        Modal.confirm({
                          title: '审核并发布当前版本？',
                          content: '请确认题目、答案、引用和附件均准确。',
                          onOk: async () => {
                            try {
                              await post(`/admin/content/${r.id}/review`, {
                                version: r.version,
                                status: 'PUBLISHED',
                                reason: '人工审核内容与引用通过',
                              });
                              void load();
                            } catch (e) {
                              message.error((e as Error).message);
                              throw e;
                            }
                          },
                        })
                      }
                    >
                      审核发布
                    </Button>
                    <Button
                      danger
                      onClick={() =>
                        post(`/admin/content/${r.id}/review`, {
                          version: r.version,
                          status: 'REJECTED',
                          reason: '内容需修改',
                        })
                          .then(load)
                          .catch((e) => message.error(e.message))
                      }
                    >
                      驳回
                    </Button>
                  </>
                )}
              </Space>
            ),
          },
        ]}
      />
      <Modal
        open={!!selected}
        onCancel={() => setSelected(null)}
        title="内容编辑 · 保存后进入待审核"
        footer={null}
        width={850}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={async (p) => {
            try {
              const body = { ...p, payload: JSON.parse(p.payload) };
              if (selected?.id) await put(`/admin/content/${selected.id}`, body);
              else await post('/admin/content', body);
              setSelected(null);
              void load();
              message.success('已保存待审核版本');
            } catch (e) {
              message.error((e as Error).message);
            }
          }}
        >
          <Form.Item name="kind" label="类型">
            <Select disabled={!!selected?.id} options={kindOptions} />
          </Form.Item>
          <Form.Item name="namespace" label="来源命名空间" rules={[{ required: true }]}>
            <Input disabled={!!selected?.id} />
          </Form.Item>
          <Form.Item name="externalId" label="稳定编码" rules={[{ required: true }]}>
            <Input disabled={!!selected?.id} />
          </Form.Item>
          <Form.Item
            name="payload"
            label="内容 JSON（支持 Markdown 正文；格式见 templates）"
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={18} style={{ fontFamily: 'monospace' }} />
          </Form.Item>
          <label>
            上传内容附件
            <input
              type="file"
              accept="application/pdf,image/png,image/jpeg,image/webp,audio/mpeg,audio/wav,audio/ogg,video/mp4"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                if (file)
                  try {
                    const r = await upload(file, 'CONTENT');
                    const p = JSON.parse(form.getFieldValue('payload'));
                    p.attachmentIds = [...(p.attachmentIds || []), r.id];
                    form.setFieldValue('payload', JSON.stringify(p, null, 2));
                  } catch (e) {
                    message.error((e as Error).message);
                  }
              }}
            />
          </label>
          <Button type="primary" htmlType="submit">
            保存新版本
          </Button>
        </Form>
      </Modal>
    </>
  );
}
function Imports() {
  const { data, error, load, pagination, isLoading } = usePageQuery('/admin/imports');

  const [preview, setPreview] = useState<Row | null>(null),
    [allow, setAllow] = useState(false),
    [busy, setBusy] = useState(false);
  const { message } = AntApp.useApp();
  const download = async (path: string, name: string) => {
    try {
      const value = await api(`/admin/imports/${path}`);
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(value, null, 2)], { type: 'application/json' }),
      );
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      message.error((e as Error).message);
    }
  };
  return (
    <>
      <h2>批量导入中心</h2>
      <Alert
        type="info"
        message="上传 → 校验查重 → 预览 → 提交 → 内容审核"
        description="导入仅创建待审核内容，不会创建会员认证记录。内容相同自动跳过，内容变化需确认生成新版本。"
      />
      <Space style={{ margin: '20px 0' }}>
        <Button onClick={() => download('schema', 'import-schema.json')}>下载 Schema</Button>
        <Button onClick={() => download('dictionary', 'dictionary.json')}>下载当前字典</Button>
      </Space>
      <input
        disabled={busy}
        type="file"
        accept=".json,.zip"
        onChange={async (e) => {
          const f = e.target.files?.[0];
          if (!f) return;
          setBusy(true);
          try {
            const form = new FormData();
            form.append('file', f);
            setPreview(
              await api('/admin/imports', {
                method: 'POST',
                headers: { 'Idempotency-Key': crypto.randomUUID() },
                body: form,
              }),
            );
            setAllow(false);
            void load();
          } catch (e) {
            message.error((e as Error).message);
          } finally {
            setBusy(false);
          }
        }}
      />
      {error && <Alert type="error" message={error} />}
      <Table
        pagination={pagination}
        loading={isLoading}
        rowKey="id"
        dataSource={data}
        columns={[
          { title: '批次', dataIndex: 'id' },
          { title: '状态', dataIndex: 'status', render: label },
          { title: '时间', dataIndex: 'createdAt' },
          {
            title: '操作',
            render: (_, r) => (
              <Button
                onClick={() => {
                  setPreview(r);
                  setAllow(false);
                }}
              >
                查看报告
              </Button>
            ),
          },
        ]}
      />
      {preview && (
        <section className="card">
          <h3>校验报告</h3>
          <JsonView
            data={typeof preview.report === 'string' ? JSON.parse(preview.report) : preview.report}
          />
          {!['COMMITTED', 'QUEUED'].includes(preview.status) && (
            <Space>
              <Checkbox checked={allow} onChange={(e) => setAllow(e.target.checked)}>
                冲突内容生成新版本
              </Checkbox>
              <Button
                type="primary"
                loading={busy}
                onClick={async () => {
                  setBusy(true);
                  try {
                    setPreview(
                      await post(`/admin/imports/${preview.id}/commit`, { allowUpdates: allow }),
                    );
                    void load();
                    message.success('已加入后台队列，可刷新查看结果');
                  } catch (e) {
                    message.error((e as Error).message);
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                确认提交
              </Button>
            </Space>
          )}
        </section>
      )}
    </>
  );
}
function Users({ role }: { role: string }) {
  const { data, error, load, pagination, isLoading } = usePageQuery('/admin/users');
  const [selected, setSelected] = useState<Row | null>(null);
  const { message } = AntApp.useApp();
  return (
    <>
      <h2>会员与身份管理</h2>
      <Alert
        type="warning"
        message="核实身份需有可靠依据，不能仅凭同名或证书图片。证书等级不会授予后台权限。"
      />
      {error && <Alert type="error" message={error} />}
      <Table
        pagination={pagination}
        loading={isLoading}
        rowKey="id"
        dataSource={data}
        columns={[
          { title: '账号', dataIndex: 'username' },
          {
            title: '身份',
            dataIndex: 'identityVerified',
            render: (v) => (v ? '已核实' : '未核实'),
          },
          {
            title: '认证等级',
            dataIndex: 'memberLevel',
            render: (v) => ['普通', '初级', '中级', '高级'][v],
          },
          { title: '角色', dataIndex: 'role' },
          {
            title: '操作',
            render: (_, r) => (
              <Space>
                {!r.identityVerified && <Button onClick={() => setSelected(r)}>核实身份</Button>}
                {role === 'ADMIN' && (
                  <>
                    <Button
                      danger
                      onClick={() =>
                        put(`/admin/users/${r.id}`, { enabled: !r.enabled })
                          .then(load)
                          .catch((e) => message.error(e.message))
                      }
                    >
                      {r.enabled ? '封禁' : '解禁'}
                    </Button>
                  </>
                )}
              </Space>
            ),
          },
        ]}
      />
      <Modal
        open={!!selected}
        title={`核实 ${selected?.username} 的身份`}
        onCancel={() => setSelected(null)}
        footer={null}
        destroyOnClose
      >
        <Form
          layout="vertical"
          onFinish={async (p) => {
            try {
              await post(`/admin/users/${selected?.id}/identity`, p);
              setSelected(null);
              void load();
            } catch (e) {
              message.error((e as Error).message);
            }
          }}
        >
          <Form.Item name="holderName" label="核实后的姓名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="evidence"
            label="身份核实方式、材料引用与依据（加密保存）"
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={5} />
          </Form.Item>
          <Button htmlType="submit" type="primary">
            保存核实记录
          </Button>
        </Form>
      </Modal>
    </>
  );
}
function Catalog() {
  const { data, error, load } = useDictionary('/public/certificates');
  const { message } = AntApp.useApp();
  return (
    <>
      <h2>证书目录</h2>
      {error && <Alert message={error} />}
      <h3>新增证书</h3>
      <Form
        layout="inline"
        onFinish={(p) =>
          post('/admin/certificates', p)
            .then(load)
            .catch((e) => message.error(e.message))
        }
      >
        {['code', 'name', 'specialty'].map((k, i) => (
          <Form.Item key={k} name={k} rules={[{ required: true }]}>
            <Input placeholder={['稳定编码', '名称', '专业'][i]} />
          </Form.Item>
        ))}
        <Form.Item name="level" initialValue={1}>
          <Select
            style={{ width: 100 }}
            options={[
              { value: 1, label: '初级' },
              { value: 2, label: '中级' },
              { value: 3, label: '高级' },
            ]}
          />
        </Form.Item>
        <Button htmlType="submit" type="primary">
          新增
        </Button>
      </Form>
      <Table
        pagination={false}
        rowKey="id"
        dataSource={data}
        columns={[
          { title: '编码', dataIndex: 'code' },
          { title: '名称', dataIndex: 'name' },
          { title: '专业', dataIndex: 'specialty' },
          { title: '级别', dataIndex: 'level', render: (x) => ['', '初级', '中级', '高级'][x] },
        ]}
      />
    </>
  );
}
function Rules() {
  const [rules, setRules] = useState<Row[]>([]);
  const { message } = AntApp.useApp();
  useEffect(() => {
    api('/admin/member-rules')
      .then(setRules)
      .catch((e) => message.error(e.message));
  }, []);
  return (
    <>
      <h2>认证等级展示配置</h2>
      <p>等级条件固定为最高有效获证级别；权益仅保存配置，首期不限制学习权限。</p>
      {rules.map((r) => (
        <Form
          key={r.level}
          initialValues={{ ...r, sort: r.sortOrder }}
          layout="inline"
          style={{ marginBottom: 20 }}
          onFinish={(p) =>
            put(`/admin/member-rules/${r.level}`, { ...p, entitlements: {} })
              .then(() => message.success('已保存'))
              .catch((e) => message.error(e.message))
          }
        >
          <Form.Item label={`等级 ${r.level}`} name="name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input />
          </Form.Item>
          <Form.Item name="sort" label="顺序">
            <InputNumber />
          </Form.Item>
          <Button htmlType="submit">保存</Button>
        </Form>
      ))}
    </>
  );
}
function Audits() {
  const { data, error, pagination, isLoading } = usePageQuery('/admin/audits');
  return (
    <>
      <h2>操作与材料访问日志</h2>
      {error && <Alert message={error} />}
      <Table
        pagination={pagination}
        loading={isLoading}
        rowKey="id"
        dataSource={data}
        columns={['createdAt', 'actorId', 'adminActorId', 'action', 'targetId', 'detail'].map(
          (k, i) => ({
            title: ['时间', '用户', '管理员', '动作', '对象', '说明'][i],
            dataIndex: k,
          }),
        )}
      />
    </>
  );
}
function Admin() {
  const { token } = useSession();
  const signed = !!token;
  const [me, setMe] = useState<Row | null>(null),
    [tab, setTab] = useState('content'),
    [error, setError] = useState('');
  useEffect(() => {
    setError('');
    setMe(null);
    if (signed)
      api('/admin/me')
        .then((x) => {
          setMe(x);
          if (x.role === 'REVIEWER') setTab('certifications');
        })
        .catch((e) => setError(e.message));
  }, [signed]);
  if (!signed) return <Login admin onLogin={() => {}} />;
  const role = me?.role;
  const items = [
    ['account', '账户设置'],
    ...(role === 'ADMIN' || role === 'EDITOR'
      ? [
          ['content', '内容管理'],
          ['catalog', '证书目录'],
          ['imports', '批量导入'],
        ]
      : []),
    ...(role === 'ADMIN' || role === 'REVIEWER'
      ? [
          ['certifications', '获证审核'],
          ['users', '会员身份'],
        ]
      : []),
    ...(role === 'ADMIN'
      ? [
          ['accounts', '后台账号'],
          ['rules', '等级配置'],
          ['audits', '操作日志'],
        ]
      : []),
  ];
  return (
    <Layout className="admin-shell">
      <div className="admin-head">
        <span className="brand">
          知阶 <small>管理后台</small>
        </span>
        <Space>
          {me?.username} · {role}
          <Button onClick={() => logout().catch((e) => setError(e.message))}>退出</Button>
        </Space>
      </div>
      <Menu
        mode="horizontal"
        selectedKeys={[tab]}
        items={items.map(([key, label]) => ({ key, label }))}
        onClick={(x) => setTab(x.key)}
      />
      <main className="admin-content">
        {error && <Alert type="error" message={error} />}{' '}
        {role === 'USER' ? (
          <Alert type="error" message="该账号没有后台权限" />
        ) : (
          role && (
            <>
              {tab === 'account' && <AccountSettings username={me?.username || ''} />}
              {tab === 'content' && <Content />}
              {tab === 'catalog' && <Catalog />}
              {tab === 'certifications' && <Certifications />}
              {tab === 'imports' && <Imports />}
              {tab === 'users' && <Users role={role} />} {tab === 'rules' && <Rules />}{' '}
              {tab === 'accounts' && <AdminAccounts currentId={me?.id || ''} />}
              {tab === 'audits' && <Audits />}
            </>
          )
        )}
      </main>
    </Layout>
  );
}
createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <SessionProvider>
        <ConfigProvider
          locale={zhCN}
          theme={{ token: { colorPrimary: '#146b60', borderRadius: 8 } }}
        >
          <AntApp>
            <Admin />
          </AntApp>
        </ConfigProvider>
      </SessionProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
