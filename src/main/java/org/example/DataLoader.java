package org.example;

import jakarta.transaction.Transactional;
import org.example.warehouse.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataLoader implements CommandLineRunner {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductPurchaseRepository purchaseRepository;

    public DataLoader(WarehouseRepository warehouseRepository, 
                      ProductRepository productRepository, 
                      ProductPurchaseRepository purchaseRepository) {
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        long warehouseCount = warehouseRepository.count();
        long productCount = productRepository.count();
        long purchaseCount = purchaseRepository.count();

        System.out.println("Database Status: Warehouses=" + warehouseCount + ", Products=" + productCount + ", Purchases=" + purchaseCount);

        if (purchaseCount > 0) {
            System.out.println("Sales data already exists. Skipping initialization.");
            return;
        }

        System.out.println("Starting data initialization...");
        Random random = new Random();
        
        // 1. Create Warehouses if none exist
        List<WarehouseEntity> warehouses;
        if (warehouseCount == 0) {
            System.out.println("Creating 8 Austrian Warehouses...");
            String[][] warehouseData = {
                {"Zentrallager Wien", "Handelskai 265", "1020", "Wien"},
                {"Logistikzentrum Linz", "Industriezeile 35", "4020", "Linz"},
                {"Hub Graz", "Puntigamer Straße 120", "8055", "Graz"},
                {"Lager Salzburg", "Kasernenstraße 1", "5020", "Salzburg"},
                {"Innsbruck West", "Innrain 52", "6020", "Innsbruck"},
                {"Klagenfurt Süd", "Völkermarkter Straße 200", "9020", "Klagenfurt"},
                {"Dornbirn Nord", "Messestraße 2", "6850", "Dornbirn"},
                {"St. Pölten Ost", "Stattersdorfer Hauptstraße 30", "3100", "St. Pölten"}
            };

            warehouses = new ArrayList<>();
            for (String[] data : warehouseData) {
                WarehouseEntity w = new WarehouseEntity();
                w.setName(data[0]);
                w.setAddress(data[1]);
                w.setPostalCode(data[2]);
                w.setCity(data[3]);
                w.setCountry("Österreich");
                w.setProducts(new ArrayList<>());
                w.setTimestamp(LocalDateTime.now());
                warehouses.add(warehouseRepository.save(w));
            }
        } else {
            warehouses = warehouseRepository.findAll();
        }

        // 2. Create Products if none exist
        List<ProductEntity> products;
        if (productCount == 0) {
            System.out.println("Creating 250 German Products...");
            String[] categories = {"Obst & Gemüse", "Milchprodukte", "Getränke", "Backwaren", "Fleisch", "Tiefkühlkost", "Snacks", "Gewürze"};
            String[] units = {"Packung", "Flasche", "kg", "Stück", "Dose", "Becher", "Glas"};
            String[] baseNames = {"Apfel", "Banane", "Milch", "Brot", "Käse", "Wurst", "Saft", "Wasser", "Ei", "Butter", "Joghurt", "Nudeln", "Reis", "Tomaten", "Gurken", "Salat", "Zwiebeln", "Kartoffeln", "Kaffee", "Tee", "Honig", "Schokolade", "Chips", "Mehl", "Zucker", "Salz", "Öl", "Essig", "Senf", "Ketchup", "Suppe", "Pizza", "Spinat", "Lachs", "Karotten", "Zitrone", "Orange", "Birne", "Erdbeeren", "Knoblauch", "Gouda", "Quark", "Sahne", "Tofu", "Oliven", "Mandeln", "Bier", "Wein", "Cola", "Eistee"};
            
            products = new ArrayList<>();
            for (int i = 1; i <= 250; i++) {
                ProductEntity p = new ProductEntity();
                String prefix = (i % 3 == 0) ? "Premium" : (i % 2 == 0) ? "Bio" : "Günstig";
                String name = baseNames[random.nextInt(baseNames.length)] + " " + prefix + " " + i;
                p.setName(name);
                p.setCategory(categories[random.nextInt(categories.length)]);
                p.setQuantity(random.nextInt(5000));
                p.setUnit(units[random.nextInt(units.length)]);
                ProductEntity savedProduct = productRepository.save(p);
                products.add(savedProduct);
                
                WarehouseEntity target = warehouses.get(random.nextInt(warehouses.size()));
                target.getProducts().add(savedProduct);
            }
            warehouseRepository.saveAll(warehouses);
        } else {
            products = productRepository.findAll();
        }

        // 3. Create 300 Purchase Records
        System.out.println("Creating 300 Purchase Records...");
        for (int i = 0; i < 300; i++) {
            ProductPurchaseEntity purchase = new ProductPurchaseEntity();
            ProductEntity randomProduct = products.get(random.nextInt(products.size()));
            WarehouseEntity randomWarehouse = warehouses.get(random.nextInt(warehouses.size()));
            
            purchase.setProduct(randomProduct);
            purchase.setWarehouse(randomWarehouse);
            purchase.setAmount(random.nextInt(50) + 1);
            purchase.setTimestamp(LocalDateTime.now().minusDays(random.nextInt(365)));
            purchaseRepository.save(purchase);
        }
        System.out.println("Data initialization finished successfully! Total purchases: " + purchaseRepository.count());
    }
}
