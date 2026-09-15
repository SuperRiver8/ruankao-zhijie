import { useState } from 'react';
import { Form, Input, Button, App } from 'antd';
import { request, updateSession } from './shared/api/client';
import { queryClient } from './shared/auth/SessionProvider';

export function AccountSettings({ username }: { username: string }) {
  const [busy, setBusy] = useState(false);
  const { message } = App.useApp();
  return (
    <section className="account-settings">
      <h2>账户设置</h2>
      <p>修改账号和密码后，所有设备需要重新登录。</p>
      <Form
        layout="vertical"
        initialValues={{ username }}
        onFinish={async (values) => {
          setBusy(true);
          try {
            await request('/admin/me/account', {
              method: 'PUT',
              data: {
                username: values.username,
                currentPassword: values.currentPassword,
                newPassword: values.newPassword,
              },
            });
            updateSession(null);
            queryClient.clear();
            message.success('已修改，请使用新账号密码登录');
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
              message: '请输入3–40位字母、数字或下划线',
            },
          ]}
        >
          <Input autoComplete="username" />
        </Form.Item>
        <Form.Item name="currentPassword" label="当前密码" rules={[{ required: true }]}>
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[{ required: true, min: 8, message: '新密码至少8位' }]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认新密码"
          dependencies={['newPassword']}
          rules={[
            { required: true },
            ({ getFieldValue }) => ({
              validator: (_, value) =>
                value === getFieldValue('newPassword')
                  ? Promise.resolve()
                  : Promise.reject(new Error('两次密码不一致')),
            }),
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={busy}>
          保存并重新登录
        </Button>
      </Form>
    </section>
  );
}
