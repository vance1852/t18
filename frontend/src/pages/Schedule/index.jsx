import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Row,
  Col,
  Card,
  Button,
  DatePicker,
  Table,
  Tag,
  Space,
  Tooltip,
  message,
  Spin,
} from "antd";
import {
  PlayCircleOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
import {
  getEquipmentList,
  getEquipmentTypes,
  getSchedule,
  generateSchedule,
  optimizeSchedule,
  updateScheduleTask,
  getOrders,
  getDishes,
} from "@/api";
import {
  getTaskColor,
  minutesToTime,
  dateTimeToMinutes,
  minutesToDateTime,
  getDurationMinutes,
  colorIndexFromId,
} from "@/utils";

const START_HOUR = 6;
const END_HOUR = 24;
const TOTAL_MINUTES = (END_HOUR - START_HOUR) * 60;
const PIXEL_PER_MINUTE = 1.2;
const ROW_HEIGHT = 50;
const ROW_HEADER_WIDTH = 140;
const GANTT_WIDTH = TOTAL_MINUTES * PIXEL_PER_MINUTE;

const Schedule = () => {
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(dayjs());
  const [tasks, setTasks] = useState([]);
  const [orders, setOrders] = useState([]);
  const [dishes, setDishes] = useState([]);
  const [equipments, setEquipments] = useState([]);
  const [equipmentTypes, setEquipmentTypes] = useState([]);
  const [draggingTask, setDraggingTask] = useState(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [previewStyle, setPreviewStyle] = useState(null);
  const [originalTask, setOriginalTask] = useState(null);
  const ganttContainerRef = useRef(null);
  const [generating, setGenerating] = useState(false);
  const [optimizing, setOptimizing] = useState(false);

  const getDishName = (dishId) => {
    const dish = dishes.find((d) => d.id === dishId);
    return dish ? dish.name : `菜品${dishId}`;
  };

  const fetchData = async () => {
    try {
      setLoading(true);
      const [eqTypesRes, eqListRes, scheduleRes, ordersRes, dishesRes] =
        await Promise.all([
          getEquipmentTypes(),
          getEquipmentList(),
          getSchedule(),
          getOrders(),
          getDishes(),
        ]);

      const types = eqTypesRes.data || [];
      const eqList = eqListRes.data || [];
      const scheduleData = scheduleRes.data || {};
      const taskList = scheduleData.tasks || scheduleRes.data || [];
      const orderList = ordersRes.data || [];
      const dishList = dishesRes.data || [];

      setEquipmentTypes(types);
      setEquipments(eqList);
      setOrders(orderList);
      setDishes(dishList);

      const getDishNameLocal = (dishId) => {
        const dish = dishList.find((d) => d.id === dishId);
        return dish ? dish.name : `菜品${dishId}`;
      };

      const formattedTasks = taskList.map((task) => {
        const startTime = dateTimeToMinutes(task.startTime, START_HOUR);
        const duration = getDurationMinutes(task.startTime, task.endTime);
        const setupTime = getDurationMinutes(task.startTime, task.setupEndTime);
        const colorIndex = colorIndexFromId(task.dishId);

        return {
          id: task.id,
          orderId: task.orderId,
          orderNo: task.orderNo,
          dishId: task.dishId,
          dishName: task.dishName || getDishNameLocal(task.dishId),
          equipmentId: task.equipmentId,
          equipmentName: task.equipmentName,
          process: task.processName,
          startTime,
          duration,
          setupTime,
          quantity: 0,
          deliveryTime: "",
          isDelayed: task.isDelayed,
          colorIndex,
          status: task.status,
        };
      });

      setTasks(formattedTasks);
    } catch (error) {
      message.error("获取排产数据失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedDate]);

  const getGroupedEquipments = () => {
    const groups = {};
    equipmentTypes.forEach((type) => {
      groups[type.name] = equipments
        .filter((eq) => eq.equipmentTypeId === type.id)
        .map((eq) => ({
          id: eq.id,
          name: eq.name,
          group: type.name,
          status: eq.status,
        }));
    });
    return groups;
  };

  const getAllEquipmentsFlat = () => {
    const grouped = getGroupedEquipments();
    const result = [];
    Object.values(grouped).forEach((eqList) => {
      result.push(...eqList);
    });
    return result;
  };

  const getEquipmentRowIndex = (equipmentId) => {
    const flatList = getAllEquipmentsFlat();
    return flatList.findIndex((eq) => eq.id === equipmentId);
  };

  const handleMouseDown = (e, task) => {
    e.preventDefault();
    const rect = e.currentTarget.getBoundingClientRect();
    setDraggingTask(task);
    setOriginalTask({ ...task });
    setDragOffset({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    });
    setPreviewStyle({
      left: e.clientX - dragOffset.x,
      top: e.clientY - dragOffset.y,
      width: rect.width,
      height: rect.height,
      backgroundColor: getTaskColor(task.colorIndex),
    });
  };

  const handleMouseMove = useCallback(
    (e) => {
      if (!draggingTask || !ganttContainerRef.current) return;

      const containerRect = ganttContainerRef.current.getBoundingClientRect();
      const relativeX = e.clientX - containerRect.left - ROW_HEADER_WIDTH;
      const relativeY = e.clientY - containerRect.top;

      let newStartTime = Math.round(
        (relativeX + dragOffset.x) / PIXEL_PER_MINUTE,
      );
      newStartTime = Math.max(
        0,
        Math.min(TOTAL_MINUTES - draggingTask.duration, newStartTime),
      );

      const flatEquipments = getAllEquipmentsFlat();
      const rowIndex = Math.floor(relativeY / ROW_HEIGHT);
      const equipmentIndex = Math.max(
        0,
        Math.min(flatEquipments.length - 1, rowIndex),
      );
      const newEquipment = flatEquipments[equipmentIndex];

      const left = newStartTime * PIXEL_PER_MINUTE;
      const top = equipmentIndex * ROW_HEIGHT;

      setPreviewStyle({
        left: containerRect.left + ROW_HEADER_WIDTH + left,
        top: containerRect.top + top + (ROW_HEIGHT - 36) / 2,
        width: draggingTask.duration * PIXEL_PER_MINUTE,
        height: 36,
        backgroundColor: getTaskColor(draggingTask.colorIndex),
        opacity: 0.7,
      });

      setTasks((prev) =>
        prev.map((t) =>
          t.id === draggingTask.id
            ? { ...t, startTime: newStartTime, equipmentId: newEquipment.id }
            : t,
        ),
      );
    },
    [draggingTask, dragOffset, equipments, equipmentTypes],
  );

  const handleMouseUp = useCallback(
    async (e) => {
      if (!draggingTask) return;

      const movedTask = tasks.find((t) => t.id === draggingTask.id);
      const hasConflict = checkConflict(movedTask);

      if (hasConflict) {
        message.error("排产冲突，已还原");
        setTasks((prev) =>
          prev.map((t) => (t.id === draggingTask.id ? { ...originalTask } : t)),
        );
      } else {
        try {
          const dateStr = selectedDate.format("YYYY-MM-DD");
          const newStartTimeStr = minutesToDateTime(
            movedTask.startTime,
            dateStr,
            START_HOUR,
          );
          const newEndTimeStr = minutesToDateTime(
            movedTask.startTime + movedTask.duration,
            dateStr,
            START_HOUR,
          );

          await updateScheduleTask(movedTask.id, {
            startTime: newStartTimeStr,
            endTime: newEndTimeStr,
            equipmentId: movedTask.equipmentId,
          });
          message.success("排产更新成功");
        } catch (error) {
          message.error("更新失败，已还原");
          setTasks((prev) =>
            prev.map((t) =>
              t.id === draggingTask.id ? { ...originalTask } : t,
            ),
          );
        }
      }

      setDraggingTask(null);
      setPreviewStyle(null);
      setOriginalTask(null);
    },
    [draggingTask, tasks, originalTask, selectedDate],
  );

  const checkConflict = (task) => {
    const sameEquipTasks = tasks.filter(
      (t) => t.equipmentId === task.equipmentId && t.id !== task.id,
    );
    for (const other of sameEquipTasks) {
      const taskStart = task.startTime;
      const taskEnd = task.startTime + task.duration;
      const otherStart = other.startTime;
      const otherEnd = other.startTime + other.duration;
      if (taskStart < otherEnd && taskEnd > otherStart) {
        return true;
      }
    }
    return false;
  };

  useEffect(() => {
    if (draggingTask) {
      window.addEventListener("mousemove", handleMouseMove);
      window.addEventListener("mouseup", handleMouseUp);
      return () => {
        window.removeEventListener("mousemove", handleMouseMove);
        window.removeEventListener("mouseup", handleMouseUp);
      };
    }
  }, [draggingTask, handleMouseMove, handleMouseUp]);

  const handleGenerateSchedule = async () => {
    try {
      setGenerating(true);
      const pendingOrders = orders.filter((o) => o.status === "PENDING");
      const orderIds = pendingOrders.map((o) => o.id);
      await generateSchedule(orderIds);
      await fetchData();
      message.success("排产生成成功");
    } catch (error) {
      message.error("生成排产失败");
    } finally {
      setGenerating(false);
    }
  };

  const handleOptimizeSchedule = async () => {
    try {
      setOptimizing(true);
      await optimizeSchedule();
      await fetchData();
      message.success("排产优化完成");
    } catch (error) {
      message.error("优化排产失败");
    } finally {
      setOptimizing(false);
    }
  };

  const timeLabels = [];
  for (let h = START_HOUR; h <= END_HOUR; h += 2) {
    timeLabels.push({
      time: `${String(h).padStart(2, "0")}:00`,
      left: (h - START_HOUR) * 60 * PIXEL_PER_MINUTE,
    });
  }

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
      title: "交期",
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
        <Tag
          color={status === "DELAYED" ? "red" : status === "COMPLETED" ? "green" : "blue"}
        >
          {status === "DELAYED"
            ? "延误"
            : status === "COMPLETED"
            ? "已完成"
            : status === "SCHEDULED"
            ? "已排产"
            : "待排产"}
        </Tag>
      ),
    },
  ];

  const groupedEquipments = getGroupedEquipments();
  const equipmentRows = [];

  Object.entries(groupedEquipments).forEach(([groupName, eqList]) => {
    equipmentRows.push({
      type: "group",
      name: groupName,
      height: ROW_HEIGHT,
    });
    eqList.forEach((eq) => {
      equipmentRows.push({
        type: "equipment",
        equipment: eq,
        height: ROW_HEIGHT,
      });
    });
  });

  const getTaskTop = (equipmentId) => {
    let top = 0;
    for (const row of equipmentRows) {
      if (row.type === "equipment" && row.equipment.id === equipmentId) {
        return top;
      }
      top += row.height;
    }
    return 0;
  };

  const renderTooltipContent = (task) => {
    const startTimeStr = minutesToTime(task.startTime + START_HOUR * 60);
    const endTimeStr = minutesToTime(
      task.startTime + task.duration + START_HOUR * 60,
    );
    return (
      <div style={{ fontSize: 12 }}>
        <p>
          <strong>订单号：</strong>
          {task.orderNo}
        </p>
        <p>
          <strong>菜品：</strong>
          {task.dishName}
        </p>
        <p>
          <strong>工序：</strong>
          {task.process}
        </p>
        <p>
          <strong>设备：</strong>
          {task.equipmentName}
        </p>
        <p>
          <strong>开始时间：</strong>
          {startTimeStr}
        </p>
        <p>
          <strong>结束时间：</strong>
          {endTimeStr}
        </p>
        <p>
          <strong>换型时间：</strong>
          {task.setupTime}分钟
        </p>
      </div>
    );
  };

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "100px" }}>
        <Spin size="large" />
      </div>
    );
  }

  const ganttHeight = equipmentRows.length * ROW_HEIGHT + 30;

  return (
    <div>
      <h2 className="page-title">生产排产</h2>

      <Card style={{ marginBottom: 16 }}>
        <Row justify="space-between" align="middle">
          <Col>
            <Space>
              <DatePicker
                value={selectedDate}
                onChange={(date) => setSelectedDate(date)}
                style={{ width: 200 }}
              />
            </Space>
          </Col>
          <Col>
            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleGenerateSchedule}
                loading={generating}
              >
                生成排产
              </Button>
              <Button
                icon={<ThunderboltOutlined />}
                onClick={handleOptimizeSchedule}
                loading={optimizing}
              >
                优化排产
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card
        title="排产甘特图"
        extra={
          <Space size="small">
            <div
              style={{
                display: "inline-block",
                width: 16,
                height: 16,
                background: "#ff4d4f",
                borderRadius: 2,
              }}
            />
            <span style={{ fontSize: 12 }}>延误任务</span>
            <div
              style={{
                display: "inline-block",
                width: 16,
                height: 16,
                background: "rgba(0,0,0,0.2)",
                borderRight: "1px dashed rgba(255,255,255,0.5)",
                borderRadius: 2,
                marginLeft: 12,
              }}
            />
            <span style={{ fontSize: 12 }}>换型时间</span>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <div
          className="gantt-container"
          ref={ganttContainerRef}
          style={{ height: ganttHeight, overflow: "auto" }}
        >
          <div
            className="gantt-header"
            style={{ width: ROW_HEADER_WIDTH + GANTT_WIDTH }}
          >
            <div
              style={{
                float: "left",
                width: ROW_HEADER_WIDTH,
                height: 30,
                background: "#fafafa",
                borderRight: "1px solid #e8e8e8",
              }}
            />
            <div
              className="gantt-timeline"
              style={{ marginLeft: ROW_HEADER_WIDTH }}
            >
              {timeLabels.map((label) => (
                <div
                  key={label.time}
                  className="gantt-time-label"
                  style={{ left: label.left }}
                >
                  {label.time}
                </div>
              ))}
            </div>
          </div>

          <div
            className="gantt-body"
            style={{
              width: ROW_HEADER_WIDTH + GANTT_WIDTH,
              clear: "both",
              position: "relative",
            }}
          >
            {equipmentRows.map((row, rowIndex) => (
              <div
                key={rowIndex}
                className={`gantt-row ${row.type === "group" ? "device-group-header" : ""}`}
                style={{ height: row.height, clear: "both" }}
              >
                <div
                  className="gantt-row-header"
                  style={{
                    height: row.height,
                    fontWeight: row.type === "group" ? 600 : "normal",
                    background: row.type === "group" ? "#e6f7ff" : "#fafafa",
                    color: row.type === "group" ? "#1890ff" : "#262626",
                  }}
                >
                  {row.type === "group" ? row.name : row.equipment.name}
                </div>
                <div
                  className="gantt-row-content"
                  style={{ height: row.height, position: "relative" }}
                >
                  {timeLabels.map((label) => (
                    <div
                      key={label.time + "-grid"}
                      className="gantt-grid-line"
                      style={{ left: label.left }}
                    />
                  ))}
                </div>
              </div>
            ))}

            {tasks.map((task) => {
              const top = getTaskTop(task.equipmentId);
              const left = task.startTime * PIXEL_PER_MINUTE;
              const width = task.duration * PIXEL_PER_MINUTE;
              const setupWidth = task.setupTime * PIXEL_PER_MINUTE;

              return (
                <Tooltip
                  key={task.id}
                  title={renderTooltipContent(task)}
                  placement="top"
                >
                  <div
                    className={`gantt-task ${task.isDelayed ? "delayed" : ""} ${
                      draggingTask?.id === task.id ? "dragging" : ""
                    }`}
                    style={{
                      left: ROW_HEADER_WIDTH + left,
                      top: top + (ROW_HEIGHT - 36) / 2,
                      width: width,
                      backgroundColor: getTaskColor(task.colorIndex),
                      position: "absolute",
                      zIndex: draggingTask?.id === task.id ? 1 : 2,
                    }}
                    onMouseDown={(e) => handleMouseDown(e, task)}
                  >
                    {task.setupTime > 0 && (
                      <div
                        className="setup-time"
                        style={{ width: setupWidth }}
                      />
                    )}
                    <span className="gantt-task-label">
                      {task.orderNo ? task.orderNo.slice(-6) : ""} - {task.dishName}
                    </span>
                  </div>
                </Tooltip>
              );
            })}
          </div>
        </div>
      </Card>

      <Card title="订单列表">
        <Table
          dataSource={orders}
          columns={orderColumns}
          rowKey="id"
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {previewStyle && (
        <div
          className="drag-preview"
          style={{
            position: "fixed",
            left: previewStyle.left,
            top: previewStyle.top,
            width: previewStyle.width,
            height: previewStyle.height,
            backgroundColor: previewStyle.backgroundColor,
            pointerEvents: "none",
            zIndex: 9999,
            opacity: 0.7,
          }}
        >
          {draggingTask &&
            `${draggingTask.orderNo ? draggingTask.orderNo.slice(-6) : ""} - ${draggingTask.dishName}`}
        </div>
      )}
    </div>
  );
};

export default Schedule;
