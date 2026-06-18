import axios from "axios";
import { message } from "antd";

const request = axios.create({
  baseURL: "/api",
  timeout: 30000,
});

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200 && res.code !== 0) {
      message.error(res.message || "请求失败");
      if (res.code === 401 || res.code === 403) {
        localStorage.removeItem("token");
        localStorage.removeItem("userInfo");
        window.location.href = "/login";
      }
      return Promise.reject(new Error(res.message || "请求失败"));
    }
    return res;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      if (status === 401 || status === 403) {
        localStorage.removeItem("token");
        localStorage.removeItem("userInfo");
        window.location.href = "/login";
        message.error("登录已过期，请重新登录");
      } else {
        message.error(data?.message || error.message || "请求失败");
      }
    } else {
      message.error(error.message || "网络异常");
    }
    return Promise.reject(error);
  }
);

export const login = (username, password) => {
  return request.post("/auth/login", { username, password });
};

export const getDishes = () => {
  return request.get("/dishes");
};

export const getDishDetail = (id) => {
  return request.get(`/dishes/${id}`);
};

export const getDishProcesses = (id) => {
  return request.get(`/dishes/${id}/processes`);
};

export const getEquipmentList = () => {
  return request.get("/equipment");
};

export const getEquipmentTypes = () => {
  return request.get("/equipment/types");
};

export const getEquipmentDetail = (id) => {
  return request.get(`/equipment/${id}`);
};

export const getOrders = () => {
  return request.get("/orders");
};

export const createOrder = (data) => {
  return request.post("/orders", data);
};

export const getOrderDetail = (id) => {
  return request.get(`/orders/${id}`);
};

export const updateOrder = (id, data) => {
  return request.put(`/orders/${id}`, data);
};

export const deleteOrder = (id) => {
  return request.delete(`/orders/${id}`);
};

export const getSchedule = () => {
  return request.get("/schedule");
};

export const generateSchedule = (orderIds) => {
  const data = orderIds ? { orderIds } : {};
  return request.post("/schedule/generate", data);
};

export const optimizeSchedule = () => {
  return request.post("/schedule/optimize");
};

export const updateScheduleTask = (taskId, data) => {
  return request.put(`/schedule/tasks/${taskId}`, data);
};

export const getMaterialList = () => {
  return request.get("/material");
};

export const getMaterialAvailability = (date) => {
  return request.get("/material/availability", { params: { date } });
};

export const getMaterialStock = (id) => {
  return request.get(`/material/${id}/stock`);
};

export const getDeliveryPlan = () => {
  return request.get("/delivery/plan");
};

export const generateDeliveryPlan = () => {
  return request.post("/delivery/plan/generate");
};

export const getDashboardSummary = (date) => {
  return request.get("/dashboard/summary", { params: { date } });
};

export const getEquipmentUtilization = (date) => {
  return request.get("/dashboard/equipment-utilization", { params: { date } });
};

export const getOnTimeDeliveryRate = (date) => {
  return request.get("/dashboard/on-time-delivery-rate", { params: { date } });
};

export const getBottleneckEquipment = (date) => {
  return request.get("/dashboard/bottleneck-equipment", { params: { date } });
};

export const getMaterialTurnover = (date) => {
  return request.get("/dashboard/material-turnover", { params: { date } });
};

export const getDailyOutput = (date) => {
  return request.get("/dashboard/daily-output", { params: { date } });
};

export default request;
