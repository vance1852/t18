import React, { useState, useEffect } from "react";
import { Row, Col, Card, Table, Tag, Statistic, Spin, message } from "antd";
import {
  RiseOutlined,
  ClockCircleOutlined,
  ToolOutlined,
  StockOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import ReactECharts from "echarts-for-react";
import dayjs from "dayjs";
import { getDashboardSummary, getOrders, getDishes } from "@/api";

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState(null);
  const [equipmentUtilizations, setEquipmentUtilizations] = useState([]);
  const [orders, setOrders] = useState([]);
  const [dishes, setDishes] = useState([]);

  const getDishName = (dishId) => {
    const dish = dishes.find((d) => d.id === dishId);
    return dish ? dish.name : `菜品${dishId}`;
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const today = dayjs().format("YYYY-MM-DD");
        const [summaryRes, ordersRes, dishesRes] = await Promise.all([
          getDashboardSummary(today),
          getOrders(),
          getDishes(),
        ]);

        const summaryData = summaryRes.data || {};
        setSummary(summaryData);
        setEquipmentUtilizations(summaryData.equipmentUtilizations || []);
        setOrders(ordersRes.data || []);
        setDishes(dishesRes.data || []);
      } catch (error) {
        message.error("获取数据失败");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const getChartOption = () => {
    const equipmentData = equipmentUtilizations || [];
    const bottleneckId = summary?.bottleneckEquipment?.id;
    return {
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "shadow",
        },
        formatter: (params) => {
          const item = params[0];
          const eq = equipmentData[item.dataIndex];
          const isBottleneck = eq && bottleneckId && eq.id === bottleneckId;
          return `${eq?.equipmentName || eq?.name || item.name}<br/>利用率: ${item.value}%${
            isBottleneck
              ? '<br/><span style="color:#ff4d4f">瓶颈设备</span>'
              : ""
          }`;
        },
      },
      grid: {
        left: "3%",
        right: "4%",
        bottom: "3%",
        containLabel: true,
      },
      xAxis: {
        type: "category",
        data: equipmentData.map((item) => item.equipmentName || item.name),
        axisLabel: {
          interval: 0,
          rotate: 30,
          fontSize: 11,
        },
      },
      yAxis: {
        type: "value",
        max: 100,
        axisLabel: {
          formatter: "{value}%",
        },
      },
      series: [
        {
          name: "利用率",
          type: "bar",
          data: equipmentData.map((item) => ({
            value: item.utilizationRate || item.utilization || 0,
            itemStyle: {
              color: bottleneckId && item.id === bottleneckId ? "#ff4d4f" : "#1890ff",
            },
          })),
          barWidth: "50%",
          label: {
            show: true,
            position: "top",
            formatter: "{c}%",
            fontSize: 11,
          },
          markLine: {
            silent: true,
            lineStyle: {
              type: "dashed",
              color: "#faad14",
            },
            data: [
              {
                yAxis: 90,
                label: {
                  formatter: "警戒线 90%",
                  fontSize: 11,
                },
              },
            ],
          },
        },
      ],
    };
  };

  const orderColumns = [
    {
      title: "订单号",
      dataIndex: "orderNo",
      key: "orderNo",
      width: 160,
    },
    {
      title: "菜品名称",
      dataIndex: "dishId",
      key: "dishName",
      render: (dishId) => getDishName(dishId),
    },
    {
      title: "数量(份)",
      dataIndex: "quantity",
      key: "quantity",
      width: 100,
    },
    {
      title: "交付时间",
      dataIndex: "deliveryEndTime",
      key: "deliveryEndTime",
      width: 160,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status) => (
        <Tag color={status === "DELAYED" ? "red" : "green"}>
          {status === "DELAYED" ? "延误" : status === "COMPLETED" ? "已完成" : "按时"}
        </Tag>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "100px" }}>
        <Spin size="large" />
      </div>
    );
  }

  const bottleneckEquipment = summary?.bottleneckEquipment;
  const bottleneckName = bottleneckEquipment?.equipmentName || bottleneckEquipment?.name || "无";

  return (
    <div>
      <h2 className="page-title">运营看板</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="当日产量(份)"
              value={summary?.dailyOutput || 0}
              prefix={<RiseOutlined style={{ color: "#1890ff" }} />}
              valueStyle={{ color: "#1890ff" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="准时交付率"
              value={summary?.onTimeDeliveryRate || 0}
              suffix="%"
              precision={1}
              prefix={<ClockCircleOutlined style={{ color: "#52c41a" }} />}
              valueStyle={{ color: "#52c41a" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="设备平均利用率"
              value={summary?.avgEquipmentUtilization || 0}
              suffix="%"
              precision={1}
              prefix={<ToolOutlined style={{ color: "#722ed1" }} />}
              valueStyle={{ color: "#722ed1" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="原料齐套率"
              value={summary?.materialAvailabilityRate || 0}
              suffix="%"
              precision={1}
              prefix={<StockOutlined style={{ color: "#fa8c16" }} />}
              valueStyle={{ color: "#fa8c16" }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={16}>
          <Card
            title="设备利用率"
            extra={
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  color: "#ff4d4f",
                }}
              >
                <WarningOutlined />
                <span>
                  瓶颈设备: {bottleneckName}
                </span>
              </div>
            }
            style={{ height: "100%" }}
          >
            <ReactECharts option={getChartOption()} style={{ height: 320 }} />
          </Card>
        </Col>
        <Col span={8}>
          <Card title="当日订单列表" style={{ height: "100%" }}>
            <Table
              dataSource={orders}
              columns={orderColumns}
              rowKey="id"
              size="small"
              pagination={false}
              scroll={{ y: 280 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;