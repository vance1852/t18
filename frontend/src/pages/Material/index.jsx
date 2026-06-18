import React, { useState, useEffect } from "react";
import { Card, Table, Tag, Spin, message, Button, Space } from "antd";
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import { getMaterialAvailability, getMaterialStock } from "@/api";

const Material = () => {
  const [loading, setLoading] = useState(true);
  const [materials, setMaterials] = useState([]);
  const [expandedRowKeys, setExpandedRowKeys] = useState([]);
  const [stockCache, setStockCache] = useState({});
  const [loadingStock, setLoadingStock] = useState({});

  const fetchData = async () => {
    try {
      setLoading(true);
      const today = dayjs().format("YYYY-MM-DD");
      const res = await getMaterialAvailability(today);
      const data = Array.isArray(res.data) ? res.data : [];

      const formatted = data.map((item) => ({
        id: item.materialId,
        materialId: item.materialId,
        name: item.materialName,
        code: item.materialCode,
        unit: item.unit,
        requiredTotal: item.requiredQuantity,
        currentStock: item.stockQuantity,
        shortage: item.shortageQuantity,
        shortageTime: item.shortageTime,
        status: item.status,
      }));

      setMaterials(formatted);
    } catch (error) {
      message.error("获取物料数据失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const loadStockBatches = async (materialId) => {
    if (stockCache[materialId]) {
      return stockCache[materialId];
    }

    try {
      setLoadingStock((prev) => ({ ...prev, [materialId]: true }));
      const res = await getMaterialStock(materialId);
      const batches = Array.isArray(res.data) ? res.data : [];

      const formatted = batches
        .map((batch) => ({
          id: batch.id,
          batchNo: `BATCH-${batch.id}`,
          materialId: batch.materialId,
          quantity: batch.quantity,
          inboundTime: batch.inboundDate,
          expiryDate: batch.expireDate,
        }))
        .sort((a, b) => {
          return new Date(a.expiryDate) - new Date(b.expiryDate);
        });

      setStockCache((prev) => ({ ...prev, [materialId]: formatted }));
      return formatted;
    } catch (error) {
      console.error("获取库存批次失败:", error);
      return [];
    } finally {
      setLoadingStock((prev) => ({ ...prev, [materialId]: false }));
    }
  };

  const handleExpand = async (expanded, record) => {
    if (expanded) {
      await loadStockBatches(record.materialId);
      setExpandedRowKeys((prev) => [...prev, record.id]);
    } else {
      setExpandedRowKeys((prev) => prev.filter((key) => key !== record.id));
    }
  };

  const handleRefresh = async () => {
    setStockCache({});
    setExpandedRowKeys([]);
    await fetchData();
    message.success("刷新成功");
  };

  const getStatusTag = (status) => {
    switch (status) {
      case "SUFFICIENT":
        return (
          <Tag color="green" icon={<CheckCircleOutlined />}>
            充足
          </Tag>
        );
      case "TIGHT":
        return (
          <Tag color="orange" icon={<WarningOutlined />}>
            紧张
          </Tag>
        );
      case "SHORTAGE":
        return (
          <Tag color="red" icon={<ExclamationCircleOutlined />}>
            不足
          </Tag>
        );
      default:
        return <Tag>未知</Tag>;
    }
  };

  const expandedRowRender = (record) => {
    const columns = [
      {
        title: "批次号",
        dataIndex: "batchNo",
        key: "batchNo",
        width: 140,
      },
      {
        title: "入库时间",
        dataIndex: "inboundTime",
        key: "inboundTime",
        width: 180,
      },
      {
        title: "保质期至",
        dataIndex: "expiryDate",
        key: "expiryDate",
        width: 140,
        render: (text) => <span style={{ color: "#fa8c16" }}>{text}</span>,
      },
      {
        title: `数量(${record.unit})`,
        dataIndex: "quantity",
        key: "quantity",
        width: 120,
      },
      {
        title: "使用顺序",
        key: "order",
        width: 100,
        render: (_, __, index) => (
          <Tag color={index === 0 ? "blue" : "default"}>
            {index === 0 ? "优先使用" : `第${index + 1}批`}
          </Tag>
        ),
      },
    ];

    const batches = stockCache[record.materialId] || [];
    const isLoading = loadingStock[record.materialId];

    if (isLoading && batches.length === 0) {
      return (
        <div style={{ textAlign: "center", padding: 20 }}>
          <Spin size="small" />
        </div>
      );
    }

    return (
      <div style={{ paddingLeft: 24 }}>
        <p style={{ color: "#595959", marginBottom: 12, fontSize: 13 }}>
          库存批次（共 {batches.length} 批，按 FEFO 先到期先用排序）
        </p>
        <Table
          columns={columns}
          dataSource={batches}
          rowKey="id"
          pagination={false}
          size="small"
        />
      </div>
    );
  };

  const columns = [
    {
      title: "原料名称",
      dataIndex: "name",
      key: "name",
      width: 150,
      render: (text, record) => (
        <span style={{ fontWeight: 500 }}>
          {text}
          <span style={{ color: "#8c8c8c", marginLeft: 8, fontSize: 12 }}>
            ({record.unit})
          </span>
        </span>
      ),
    },
    {
      title: "需求总量",
      dataIndex: "requiredTotal",
      key: "requiredTotal",
      width: 120,
      align: "right",
      render: (value, record) => `${value} ${record.unit}`,
    },
    {
      title: "当前库存",
      dataIndex: "currentStock",
      key: "currentStock",
      width: 120,
      align: "right",
      render: (value, record) => `${value} ${record.unit}`,
    },
    {
      title: "缺口量",
      dataIndex: "shortage",
      key: "shortage",
      width: 120,
      align: "right",
      render: (value, record) => (
        <span
          style={{ color: value > 0 ? "#ff4d4f" : "#52c41a", fontWeight: 500 }}
        >
          {value > 0 ? `-${value} ${record.unit}` : "0"}
        </span>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status) => getStatusTag(status),
    },
  ];

  const rowClassName = (record) => {
    if (record.status === "SHORTAGE") return "material-shortage";
    if (record.status === "TIGHT") return "material-tight";
    return "";
  };

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "100px" }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Card
        title="物料齐套检查"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
          </Space>
        }
      >
        <p style={{ color: "#8c8c8c", marginBottom: 16, fontSize: 13 }}>
          点击左侧箭头可展开查看库存批次明细（按 FEFO 先到期先用排序）
        </p>
        <Table
          columns={columns}
          dataSource={materials}
          rowKey="id"
          rowClassName={rowClassName}
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

export default Material;
