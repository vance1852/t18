# Central Kitchen Production Scheduling System

中央厨房预制菜生产排产系统，包含生产排产、物料齐套、冷链配送、运营看板等功能。

## 技术栈

- 后端：Java 17 + Spring Boot 3.2 + PostgreSQL
- 前端：React 18 + Ant Design 5
- 部署：Docker + Docker Compose

## 快速启动

```bash
docker-compose up -d
```

启动后访问：
- 前端：http://localhost:3000
- 后端API：http://localhost:8080
- 默认账号：admin / admin123

## 功能模块

### 1. 生产排产 (核心)
- 基于规则的车间作业调度
- 考虑工序先后约束、设备能力、换型时间
- 交期紧迫度优先 + 同类菜品连排优化
- 可拖拽甘特图，实时冲突校验
- 延误订单识别与局部改进

### 2. 物料齐套检查
- 按排产计划和BOM倒推原料需求
- 库存对照，识别缺料及缺料时点
- 原料保质期管理（先到期先用）

### 3. 冷链交付调度
- 按交付时间窗和配送区域合并批次
- 车辆排程与发车计划

### 4. 运营看板
- 设备利用率统计
- 准时交付率
- 瓶颈设备识别
- 原料周转分析
- 当日产量统计
