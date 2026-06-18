import React, { useState } from "react";
import { Layout, Menu, Avatar, Dropdown, message } from "antd";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import {
  DashboardOutlined,
  ScheduleOutlined,
  StockOutlined,
  CarOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from "@ant-design/icons";

const { Header, Sider, Content } = Layout;

const MainLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");

  const menuItems = [
    {
      key: "/dashboard",
      icon: <DashboardOutlined />,
      label: "运营看板",
    },
    {
      key: "/schedule",
      icon: <ScheduleOutlined />,
      label: "生产排产",
    },
    {
      key: "/material",
      icon: <StockOutlined />,
      label: "物料齐套",
    },
    {
      key: "/delivery",
      icon: <CarOutlined />,
      label: "冷链配送",
    },
  ];

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
    message.success("退出登录成功");
    navigate("/login");
  };

  const userMenuItems = [
    {
      key: "logout",
      icon: <LogoutOutlined />,
      label: "退出登录",
      onClick: handleLogout,
    },
  ];

  const selectedKey =
    menuItems.find((item) => location.pathname.startsWith(item.key))?.key ||
    "/dashboard";

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="dark"
        width={220}
      >
        <div
          style={{
            height: 64,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#fff",
            fontSize: collapsed ? 14 : 18,
            fontWeight: 600,
            background: "rgba(255, 255, 255, 0.1)",
          }}
        >
          {collapsed ? "排产" : "中央厨房排产系统"}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: "0 24px",
            background: "#fff",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            boxShadow: "0 1px 4px rgba(0, 21, 41, 0.08)",
          }}
        >
          <div
            style={{ cursor: "pointer" }}
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </div>
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div
              style={{
                display: "flex",
                alignItems: "center",
                cursor: "pointer",
                gap: 8,
              }}
            >
              <Avatar icon={<UserOutlined />} />
              <span>{userInfo.username || "管理员"}</span>
            </div>
          </Dropdown>
        </Header>
        <Content
          style={{
            margin: 0,
            padding: 24,
            minHeight: 280,
            background: "#f0f2f5",
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
