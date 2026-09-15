import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Form, Input, Modal, Select, Space, Switch, Table, Tag } from 'antd';
import { request } from './shared/api/client';
import type { components } from './shared/types/generated';

type Account = components['schemas']['AdminResponse'];
type CreateAccount = components['schemas']['AdminCreateRequest'];
type UpdateAccount = components['schemas']['AdminUpdateRequest'];
interface Page<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}
const roleOptions = ['ADMIN', 'EDITOR', 'REVIEWER'].map((value) => ({
  value,
  label: { ADMIN: '管理员', EDITOR: '内容编辑', REVIEWER: '认证审核' }[value],
}));
export function AdminAccounts({ currentId }: { currentId: string }) {
  const [params, setParams] = useSearchParams();
  const [selected, setSelected] = useState<Account | null>(null);
  const [open, setOpen] = useState(false),
    [busy, setBusy] = useState(false);
  const [form] = Form.useForm<CreateAccount>();
  const { message } = App.useApp();
  const cache = useQueryClient();
  const page = Math.max(1, Number(params.get('accountsPage')) || 1),
    size = 20;
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  for (const key of ['username', 'role', 'enabled']) {
    const value = params.get('accounts' + key);
    if (value) query.set(key, value);
  }
  const result = useQuery({
    queryKey: ['admin-accounts', query.toString()],
    queryFn: () => request<Page<Account>>('/admin/accounts?' + query),
  });
  const edit = (account: Account | null) => {
    setSelected(account);
    form.resetFields();
    form.setFieldsValue(
      account
        ? { username: account.username, role: account.role, enabled: account.enabled }
        : { role: 'EDITOR', enabled: true },
    );
    setOpen(true);
  };
  return (
    <>
      <div className="section-title">
        <h2>后台账号</h2>
        <Button type="primary" onClick={() => edit(null)}>
          新增后台账号
        </Button>
      </div>
      <Form
        layout="inline"
        initialValues={{
          username: params.get('accountsusername'),
          role: params.get('accountsrole') || undefined,
          enabled: params.get('accountsenabled') || undefined,
        }}
        onFinish={(values) =>
          setParams((previous) => {
            const next = new URLSearchParams(previous);
            for (const key of ['username', 'role', 'enabled']) {
              if (values[key]) next.set('accounts' + key, String(values[key]));
              else next.delete('accounts' + key);
            }
            next.set('accountsPage', '1');
            return next;
          })
        }
      >
        <Form.Item name="username">
          <Input placeholder="搜索账号" allowClear />
        </Form.Item>
        <Form.Item name="role">
          <Select placeholder="角色" allowClear options={roleOptions} style={{ width: 150 }} />
        </Form.Item>
        <Form.Item name="enabled">
          <Select
            placeholder="启用状态"
            allowClear
            options={[
              { value: 'true', label: '启用' },
              { value: 'false', label: '禁用' },
            ]}
            style={{ width: 130 }}
          />
        </Form.Item>
        <Button htmlType="submit">查询</Button>
      </Form>
      {result.error && <Alert type="error" message={result.error.message} />}
      <Table<Account>
        rowKey="id"
        loading={result.isPending}
        dataSource={result.data?.records || []}
        pagination={{
          current: page,
          pageSize: size,
          total: result.data?.total || 0,
          showSizeChanger: false,
          onChange: (value) =>
            setParams((previous) => {
              const next = new URLSearchParams(previous);
              next.set('accountsPage', String(value));
              return next;
            }),
        }}
        columns={[
          { title: '账号', dataIndex: 'username' },
          { title: '角色', dataIndex: 'role' },
          {
            title: '状态',
            dataIndex: 'enabled',
            render: (value) => <Tag color={value ? 'green' : 'red'}>{value ? '启用' : '禁用'}</Tag>,
          },
          {
            title: '创建时间',
            dataIndex: 'createdAt',
            render: (value) => (value ? new Date(value).toLocaleString() : '—'),
          },
          {
            title: '操作',
            render: (_, account) => <Button onClick={() => edit(account)}>编辑</Button>,
          },
        ]}
      />
      <Modal
        title={selected ? '编辑后台账号' : '新增后台账号'}
        open={open}
        onCancel={() => {
          if (!busy) setOpen(false);
        }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={async (values) => {
            setBusy(true);
            try {
              if (selected) {
                const data: UpdateAccount = {
                  username: values.username,
                  role: values.role,
                  enabled: values.enabled,
                };
                await request('/admin/accounts/' + selected.id, { method: 'PUT', data });
              } else await request('/admin/accounts', { method: 'POST', data: values });
              setOpen(false);
              await cache.invalidateQueries({ queryKey: ['admin-accounts'] });
              message.success('已保存');
            } catch (error) {
              message.error((error as Error).message);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Form.Item
            name="username"
            label="账号"
            rules={[
              {
                required: true,
                pattern: /^[A-Za-z0-9_]{3,40}$/,
                message: '3–40位字母、数字或下划线',
              },
            ]}
          >
            <Input autoComplete="off" />
          </Form.Item>
          {!selected && (
            <Form.Item
              name="password"
              label="初始密码"
              rules={[{ required: true, min: 8, max: 72, message: '密码为8–72位，且不超过72字节' }]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          )}
          <Form.Item name="role" label="角色" rules={[{ required: true }]}>
            <Select options={roleOptions} disabled={selected?.id === currentId} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch disabled={selected?.id === currentId} />
          </Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={busy}>
              保存
            </Button>
            <Button disabled={busy} onClick={() => setOpen(false)}>
              取消
            </Button>
          </Space>
        </Form>
      </Modal>
    </>
  );
}
