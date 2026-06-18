import React, { useState, useEffect } from 'react';
import { Card, Table, Tag, Spin, message, Button, Space, Badge } from 'antd';
import {
  CarOutlined,
  EnvironmentOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { getDeliveryPlan, generateDeliveryPlan, getOrders, getDishes } from '@/api';

const Delivery = () => {
  const [loading, setLoading] = useState(true);
  const [deliveries, setDeliveries] = useState([]);
  const [expandedRowKeys, setExpandedRowKeys] = useState([]);
  const [orders, setOrders] = useState([]);
  const [dishes, setDishes] = useState([]);
  const [generating, setGenerating] = useState(false);

  const getDishName = (dishId) => {
    const dish = dishes.find((d) => d.id === dishId);
    return dish ? dish.name : `菜品${dishId}`;
  };

  const getOrderById = (orderId) => {
    return orders.find((o) => o.id === orderId);
  };

  const parseOrderIds = (orderIdsStr) => {
    if (!orderIdsStr) return [];
    if (Array.isArray(orderIdsStr)) return orderIdsStr;
    return String(orderIdsStr)
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s)
      .map((s) => {
        const num = parseInt(s, 10);
        return isNaN(num) ? s : num;
      });
  };

  const fetchData = async () => {
    try {
      setLoading(true);
      const [deliveryRes, ordersRes, dishesRes] = await Promise.all([
        getDeliveryPlan(),
        getOrders(),
        getDishes(),
      ]);

      const deliveryData = Array.isArray(deliveryRes.data) ? deliveryRes.data : [];
      const orderList = ordersRes.data || [];
      const dishList = dishesRes.data || [];

      setOrders(orderList);
      setDishes(dishList);

      const formatted = deliveryData.map((item) => {
        const orderIds = parseOrderIds(item.orderIds);
        const batchOrders = orderIds
          .map((id) => getOrderById(id))
          .filter(Boolean);
        const totalQuantity = batchOrders.reduce(
          (sum, o) => sum + (o.quantity || 0),
          0,
        );

        return {
          id: item.id,
          batchNo: item.batchNo,
          area: item.deliveryArea,
          departureTime: item.departureTime,
          arrivalTime: item.arrivalTime,
          vehicleNo: item.vehicleNo,
          orderIds,
          orderCount: orderIds.length,
          totalQuantity: item.totalQuantity || totalQuantity,
          status: item.status || 'PENDING',
          orders: batchOrders,
        };
      });

      setDeliveries(formatted);
    } catch (error) {
      message.error('获取配送数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleExpand = (expanded, record) => {
    if (expanded) {
      setExpandedRowKeys((prev) => [...prev, record.id]);
    } else {
      setExpandedRowKeys((prev) => prev.filter((key) => key !== record.id));
    }
  };

  const handleRefresh = async () => {
    await fetchData();
    message.success('刷新成功');
  };

  const handleGenerate = async () => {
    try {
      setGenerating(true);
      await generateDeliveryPlan();
      message.success('配送计划生成成功');
      await fetchData();
    } catch (error) {
      message.error('生成配送计划失败');
    } finally {
      setGenerating(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
      case 'pending':
        return <Badge status="default" text="待发车" />;
      case 'SHIPPING':
      case 'shipping':
      case 'DELIVERING':
      case 'delivering':
        return <Badge status="processing" text="配送中" />;
      case 'DELIVERED':
      case 'delivered':
      case 'COMPLETED':
      case 'completed':
        return <Badge status="success" text="已送达" />;
      default:
        return <Badge status="default" text={status || '未知'} />;
    }
  };

  const expandedRowRender = (record) => {
    const columns = [
      {
        title: '订单号',
        dataIndex: 'orderNo',
        key: 'orderNo',
        width: 160,
      },
      {
        title: '菜品名称',
        dataIndex: 'dishId',
        key: 'dishName',
        render: (dishId) => getDishName(dishId),
      },
      {
        title: '数量(份)',
        dataIndex: 'quantity',
        key: 'quantity',
        width: 100,
      },
      {
        title: '交付开始时间',
        dataIndex: 'deliveryStartTime',
        key: 'deliveryStartTime',
        width: 180,
      },
      {
        title: '交付结束时间',
        dataIndex: 'deliveryEndTime',
        key: 'deliveryEndTime',
        width: 180,
      },
      {
        title: '配送区域',
        dataIndex: 'deliveryArea',
        key: 'deliveryArea',
        width: 120,
      },
    ];

    const batchOrders = record.orderIds
      .map((id) => getOrderById(id))
      .filter(Boolean);

    return (
      <div style={{ paddingLeft: 24 }}>
        <p style={{ color: '#595959', marginBottom: 12, fontSize: 13 }}>
          订单明细（共 {record.orderCount} 个订单）
        </p>
        <Table
          columns={columns}
          dataSource={batchOrders}
          rowKey="id"
          pagination={false}
          size="small"
        />
      </div>
    );
  };

  const columns = [
    {
      title: '批次号',
      dataIndex: 'batchNo',
      key: 'batchNo',
      width: 150,
      render: (text) => (
        <span style={{ fontWeight: 500, color: '#1890ff' }}>{text}</span>
      ),
    },
    {
      title: '配送区域',
      dataIndex: 'area',
      key: 'area',
      width: 120,
      render: (text) => (
        <span>
          <EnvironmentOutlined style={{ marginRight: 4, color: '#1890ff' }} />
          {text}
        </span>
      ),
    },
    {
      title: '发车时间',
      dataIndex: 'departureTime',
      key: 'departureTime',
      width: 180,
      render: (text) => (
        <span>
          <ClockCircleOutlined style={{ marginRight: 4, color: '#52c41a' }} />
          {text}
        </span>
      ),
    },
    {
      title: '预计到达',
      dataIndex: 'arrivalTime',
      key: 'arrivalTime',
      width: 180,
    },
    {
      title: '车辆号',
      dataIndex: 'vehicleNo',
      key: 'vehicleNo',
      width: 120,
      render: (text) => (
        <Tag icon={<CarOutlined />} color="blue">
          {text}
        </Tag>
      ),
    },
    {
      title: '包含订单数',
      dataIndex: 'orderCount',
      key: 'orderCount',
      width: 100,
      align: 'center',
      render: (count) => `${count} 单`,
    },
    {
      title: '总数量(份)',
      dataIndex: 'totalQuantity',
      key: 'totalQuantity',
      width: 120,
      align: 'right',
      render: (value) => <span style={{ fontWeight: 500 }}>{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusBadge(status),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Card
        title="冷链配送批次"
        extra={
          <Space>
            <Button
              icon={<ThunderboltOutlined />}
              onClick={handleGenerate}
              loading={generating}
            >
              生成配送计划
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
          </Space>
        }
      >
        <p style={{ color: '#8c8c8c', marginBottom: 16, fontSize: 13 }}>
          点击左侧箭头可展开查看批次内的订单明细
        </p>
        <Table
          columns={columns}
          dataSource={deliveries}
          rowKey="id"
          expandable={{
            expandedRowRender,
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys(keys),
            onExpand: handleExpand,
          }}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
};

export default Delivery;
