import dayjs from "dayjs";

export const formatTime = (date, format = "YYYY-MM-DD HH:mm:ss") => {
  return dayjs(date).format(format);
};

export const timeToMinutes = (timeStr) => {
  const [hours, minutes] = timeStr.split(":").map(Number);
  return hours * 60 + minutes;
};

export const minutesToTime = (minutes) => {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  return `${String(hours).padStart(2, "0")}:${String(mins).padStart(2, "0")}`;
};

export const getTimeOffset = (time, startHour = 6) => {
  const [hours, minutes] = time.split(":").map(Number);
  return (hours - startHour) * 60 + minutes;
};

export const getTaskColor = (index) => {
  const colors = [
    "#1890ff",
    "#52c41a",
    "#faad14",
    "#722ed1",
    "#eb2f96",
    "#13c2c2",
    "#fa8c16",
    "#2f54eb",
    "#a0d911",
    "#f5222d",
  ];
  return colors[index % colors.length];
};

export const getStatusBadge = (status) => {
  if (status === "delayed" || status === "延误") {
    return "delay-badge";
  }
  return "on-time-badge";
};

export const mockDelay = (ms = 500) => {
  return new Promise((resolve) => setTimeout(resolve, ms));
};

export const dateTimeToMinutes = (dateTimeStr, startHour = 6) => {
  const dt = dayjs(dateTimeStr);
  const hours = dt.hour();
  const minutes = dt.minute();
  const totalMinutes = hours * 60 + minutes;
  return totalMinutes - startHour * 60;
};

export const minutesToDateTime = (minutes, dateStr, startHour = 6) => {
  const totalMinutes = minutes + startHour * 60;
  const hours = Math.floor(totalMinutes / 60);
  const mins = totalMinutes % 60;
  return dayjs(dateStr).hour(hours).minute(mins).second(0).format("YYYY-MM-DD HH:mm:ss");
};

export const getDurationMinutes = (startTimeStr, endTimeStr) => {
  const start = dayjs(startTimeStr);
  const end = dayjs(endTimeStr);
  return end.diff(start, "minute");
};

export const colorIndexFromId = (id) => {
  const num = typeof id === "number" ? id : parseInt(String(id).replace(/\D/g, ""), 10) || 0;
  return num % 10;
};
