import React, { useState } from "react";
import { Form, Input, Button, message } from "antd";
import { UserOutlined, LockOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { login } from "@/api";

const Login = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const res = await login(values.username, values.password);
      const data = res.data || {};
      const token = data.token || data.accessToken;
      const userInfo = data.userInfo || data.user || { username: values.username };

      if (token) {
        localStorage.setItem("token", token);
      }
      localStorage.setItem("userInfo", JSON.stringify(userInfo));
      message.success("登录成功");
      navigate("/dashboard");
    } catch (error) {
      message.error(error.message || "登录失败，请重试");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">中央厨房生产排产系统</h1>
        <p className="login-subtitle">预制菜智能排产管理平台</p>
        <Form
          name="login"
          initialValues={{ username: "admin", password: "admin123" }}
          onFinish={onFinish}
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: "请输入用户名" }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: "请输入密码" }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
        <p style={{ textAlign: "center", color: "#bfbfbf", fontSize: 12 }}>
          默认账号: admin / admin123
        </p>
      </div>
    </div>
  );
};

export default Login;