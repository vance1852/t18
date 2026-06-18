package com.kitchen.config;

import com.kitchen.entity.*;
import com.kitchen.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EquipmentTypeRepository equipmentTypeRepository;
    private final EquipmentRepository equipmentRepository;
    private final DishRepository dishRepository;
    private final DishProcessRepository dishProcessRepository;
    private final MaterialRepository materialRepository;
    private final DishBomRepository dishBomRepository;
    private final MaterialStockRepository stockRepository;
    private final ProductionOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        initEquipmentTypes();
        initEquipment();
        initDishes();
        initDishProcesses();
        initMaterials();
        initDishBoms();
        initMaterialStock();
        initOrders();
        initAdminUser();
    }

    private void initEquipmentTypes() {
        String[][] types = {
            {"前处理设备", "用于食材清洗、切配等前处理工序"},
            {"炒制设备", "用于菜品炒制、蒸煮等热加工工序"},
            {"速冻设备", "用于菜品快速冷冻工序"},
            {"包装设备", "用于菜品封装、贴标等包装工序"}
        };

        for (String[] type : types) {
            EquipmentType equipmentType = new EquipmentType();
            equipmentType.setName(type[0]);
            equipmentType.setDescription(type[1]);
            equipmentTypeRepository.save(equipmentType);
        }
    }

    private void initEquipment() {
        List<EquipmentType> types = equipmentTypeRepository.findAll();
        String[][] equipmentNames = {
            {"前处理线1", "前处理线2", "前处理线3", "前处理线4"},
            {"炒制锅1", "炒制锅2", "炒制锅3", "炒制锅4", "炒制锅5"},
            {"速冻机1", "速冻机2", "速冻机3"},
            {"包装线1", "包装线2", "包装线3", "包装线4"}
        };

        for (int i = 0; i < types.size(); i++) {
            EquipmentType type = types.get(i);
            for (String name : equipmentNames[i]) {
                Equipment equipment = new Equipment();
                equipment.setName(name);
                equipment.setEquipmentTypeId(type.getId());
                equipment.setStatus(Equipment.EquipmentStatus.AVAILABLE);
                equipmentRepository.save(equipment);
            }
        }
    }

    private void initDishes() {
        String[][] dishes = {
            {"红烧肉", "HC001", "热菜"},
            {"宫保鸡丁", "GB001", "热菜"},
            {"鱼香肉丝", "YX001", "热菜"},
            {"麻婆豆腐", "MP001", "热菜"},
            {"糖醋排骨", "TC001", "热菜"},
            {"清蒸鲈鱼", "QC001", "热菜"},
            {"西红柿炒蛋", "XHS001", "热菜"},
            {"土豆烧牛肉", "TD001", "热菜"},
            {"酸辣土豆丝", "SL001", "素菜"},
            {"蒜蓉西兰花", "SR001", "素菜"}
        };

        for (String[] dish : dishes) {
            Dish d = new Dish();
            d.setName(dish[0]);
            d.setCode(dish[1]);
            d.setCategory(dish[2]);
            dishRepository.save(d);
        }
    }

    private void initDishProcesses() {
        List<Dish> dishes = dishRepository.findAll();
        List<EquipmentType> types = equipmentTypeRepository.findAll();

        Map<String, Integer> processTimes = new HashMap<>();
        processTimes.put("前处理", 15);
        processTimes.put("炒制", 25);
        processTimes.put("速冻", 30);
        processTimes.put("包装", 10);

        Map<String, Integer> setupTimes = new HashMap<>();
        setupTimes.put("前处理", 5);
        setupTimes.put("炒制", 10);
        setupTimes.put("速冻", 8);
        setupTimes.put("包装", 3);

        for (Dish dish : dishes) {
            int seq = 1;
            for (EquipmentType type : types) {
                String processName = "";
                if (type.getName().contains("前处理")) processName = "前处理";
                else if (type.getName().contains("炒制")) processName = "炒制";
                else if (type.getName().contains("速冻")) processName = "速冻";
                else if (type.getName().contains("包装")) processName = "包装";

                int timeVariation = random.nextInt(10) - 5;

                DishProcess process = new DishProcess();
                process.setDishId(dish.getId());
                process.setProcessName(processName);
                process.setEquipmentTypeId(type.getId());
                process.setStandardTime(processTimes.get(processName) + timeVariation);
                process.setSetupTime(setupTimes.get(processName));
                process.setSequence(seq++);
                dishProcessRepository.save(process);
            }
        }
    }

    private void initMaterials() {
        String[][] materials = {
            {"猪肉", "PORK001", "kg"},
            {"牛肉", "BEEF001", "kg"},
            {"鸡肉", "CHICKEN001", "kg"},
            {"鱼肉", "FISH001", "kg"},
            {"鸡蛋", "EGG001", "kg"},
            {"豆腐", "TOFU001", "盒"},
            {"土豆", "POTATO001", "kg"},
            {"西红柿", "TOMATO001", "kg"},
            {"西兰花", "BROCCOLI001", "kg"},
            {"青椒", "PEPPER001", "kg"},
            {"胡萝卜", "CARROT001", "kg"},
            {"洋葱", "ONION001", "kg"},
            {"大蒜", "GARLIC001", "kg"},
            {"生姜", "GINGER001", "kg"},
            {"生抽", "SOY001", "L"},
            {"老抽", "DARKSOY001", "L"},
            {"食用油", "OIL001", "L"},
            {"盐", "SALT001", "kg"},
            {"糖", "SUGAR001", "kg"},
            {"料酒", "COOKINGWINE001", "L"}
        };

        for (String[] material : materials) {
            Material m = new Material();
            m.setName(material[0]);
            m.setCode(material[1]);
            m.setUnit(material[2]);
            materialRepository.save(m);
        }
    }

    private void initDishBoms() {
        List<Dish> dishes = dishRepository.findAll();
        List<Material> materials = materialRepository.findAll();

        Map<String, int[]> dishMaterialMap = new HashMap<>();
        dishMaterialMap.put("红烧肉", new int[]{0, 14, 15, 16, 17, 18, 12, 13});
        dishMaterialMap.put("宫保鸡丁", new int[]{2, 9, 14, 16, 17, 18, 12, 13});
        dishMaterialMap.put("鱼香肉丝", new int[]{0, 9, 7, 14, 16, 17, 18, 19});
        dishMaterialMap.put("麻婆豆腐", new int[]{5, 0, 14, 15, 16, 17, 12});
        dishMaterialMap.put("糖醋排骨", new int[]{0, 18, 14, 19, 16, 12, 13});
        dishMaterialMap.put("清蒸鲈鱼", new int[]{3, 12, 13, 17, 19});
        dishMaterialMap.put("西红柿炒蛋", new int[]{7, 4, 16, 17, 18});
        dishMaterialMap.put("土豆烧牛肉", new int[]{1, 6, 14, 15, 16, 17, 12, 13});
        dishMaterialMap.put("酸辣土豆丝", new int[]{6, 9, 14, 16, 17, 18});
        dishMaterialMap.put("蒜蓉西兰花", new int[]{8, 12, 16, 17});

        double[] baseQuantities = {0.25, 0.2, 0.2, 0.3, 0.1, 0.15, 0.2, 0.2, 0.15, 0.2};

        for (int i = 0; i < dishes.size(); i++) {
            Dish dish = dishes.get(i);
            int[] materialIndices = dishMaterialMap.get(dish.getName());

            for (int j = 0; j < materialIndices.length; j++) {
                Material material = materials.get(materialIndices[j]);

                double quantity;
                if (material.getUnit().equals("kg") || material.getUnit().equals("L")) {
                    quantity = 0.05 + random.nextDouble() * 0.2;
                } else {
                    quantity = 1 + random.nextInt(2);
                }

                DishBom bom = new DishBom();
                bom.setDishId(dish.getId());
                bom.setMaterialId(material.getId());
                bom.setQuantity(Math.round(quantity * 100.0) / 100.0);
                dishBomRepository.save(bom);
            }
        }
    }

    private void initMaterialStock() {
        List<Material> materials = materialRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Material material : materials) {
            int batchCount = 3 + random.nextInt(3);

            for (int i = 0; i < batchCount; i++) {
                double quantity;
                if (material.getUnit().equals("kg") || material.getUnit().equals("L")) {
                    quantity = 50 + random.nextDouble() * 150;
                } else {
                    quantity = 100 + random.nextInt(200);
                }

                int daysToExpire = 7 + random.nextInt(30);

                MaterialStock stock = new MaterialStock();
                stock.setMaterialId(material.getId());
                stock.setQuantity(Math.round(quantity * 10.0) / 10.0);
                stock.setInboundDate(today.minusDays(1 + random.nextInt(10)));
                stock.setExpireDate(today.plusDays(daysToExpire));
                stockRepository.save(stock);
            }
        }
    }

    private void initOrders() {
        List<Dish> dishes = dishRepository.findAll();
        LocalDateTime todayStart = LocalDate.now().atTime(6, 0);

        String[] areas = {"朝阳区", "海淀区", "东城区", "西城区", "丰台区"};

        for (int i = 0; i < 18; i++) {
            Dish dish = dishes.get(random.nextInt(dishes.size()));
            int quantity = 50 + random.nextInt(200);

            int startHourOffset = 2 + random.nextInt(8);
            int durationHours = 2 + random.nextInt(4);

            LocalDateTime deliveryStart = todayStart.plusHours(startHourOffset);
            LocalDateTime deliveryEnd = deliveryStart.plusHours(durationHours);

            ProductionOrder order = new ProductionOrder();
            order.setOrderNo("PO-" + String.format("%04d", i + 1));
            order.setDishId(dish.getId());
            order.setQuantity(quantity);
            order.setDeliveryStartTime(deliveryStart);
            order.setDeliveryEndTime(deliveryEnd);
            order.setDeliveryArea(areas[random.nextInt(areas.length)]);
            order.setStatus(ProductionOrder.OrderStatus.PENDING);
            orderRepository.save(order);
        }
    }

    private void initAdminUser() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("admin123"));
        user.setRole("ADMIN");
        userRepository.save(user);
    }
}
