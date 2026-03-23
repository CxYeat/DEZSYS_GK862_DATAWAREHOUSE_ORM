# MidEng GK8.1 Spring Data and ORM [GK/EK]

---

**Datum:** *10.03.2026*

**Autor:** *Aron Handan*

---

## Questions and answer

### 1. What is ORM and how is JPA used?

* **ORM (Object-Relational Mapping)** is a technique used to bridge the gap between object-oriented programming (Java) and relational databases (SQL). It allows you to interact with your database using Java objects instead of writing raw SQL queries.

* **JPA (Jakarta Persistence API)** is the standard Java **specification** for ORM. It is not a tool itself, but a set of rules and interfaces. To use it, you need an **implementation** like **Hibernate**. JPA is used by defining Java classes as "Entities" that represent database tables.

### 2. What is `application.properties` used for and where must it be stored?

- **Purpose:** It is the central configuration file for Spring Boot applications. It is used to define settings such as database connection strings (URL, username, password), server ports, and logging levels.

- **Location:** It must be stored in the **`src/main/resources`** folder of your Maven or Gradle project.

### 3. Which annotations are frequently used for entity types? Which key points must be observed?

**Frequent Annotations:**

- `@Entity`: Marks the Java class as a database table.

- `@Id`: Specifies the primary key of the entity.

- `@GeneratedValue`: Defines how the primary key should be generated (e.g., `GenerationType.IDENTITY`).

- `@Column`: Used to customize column details (name, nullable, length).

- `@Table`: Used to specify the exact name of the table in the DB.

**Key points to observe:**

- **No-args constructor:** Every Entity must have a `public` or `protected` default constructor.

- **Primary Key:** Every Entity **must** have a field marked with `@Id`.

- **Non-final:** The class and its methods should not be `final` because JPA providers often create proxies.

### 4. What methods do you need for CRUD operations?

In Spring Data JPA, these methods are usually provided automatically by extending the `JpaRepository` or `CrudRepository` interface.

| Operation  | Method Name                         | Description                                    |
|:---------- |:----------------------------------- |:---------------------------------------------- |
| **C**reate | `save(entity)`                      | Saves a new record to the database.            |
| **R**ead   | `findById(id)` / `findAll()`        | Retrieves a specific record or all records.    |
| **U**pdate | `save(entity)`                      | Updates the record (if the ID already exists). |
| **D**elete | `deleteById(id)` / `delete(entity)` | Removes a record from the database.            |

---

## Grundlagen

### Create the database for Warehouse

#### Docker Service

```yaml
services:
  mysql:
    image: 'mysql:8.4.7'
    environment:
      - 'MYSQL_DATABASE=mydatabase'
      - 'MYSQL_PASSWORD=secret'
      - 'MYSQL_ROOT_PASSWORD=verysecret'
      - 'MYSQL_USER=myuser'
    ports:
      - '3306:3306'
```

We use a Docker container with the `mysql:8.4.7` image to initialize a consistent database environment. The setup defines essential credentials and the database name via environment variables, mapping port `3306` to ensure local access

With JPA and Entity classes, you don't need a manual schema script because Hibernate automatically generates the table structure based on your Java code

#### Create a User with cURL

![](C:\Users\aronh\AppData\Roaming\marktext\images\2026-03-22-21-31-44-image.png)  

### Create WarehouseEntity and ProductEntity

#### Core Data Model

The application implements a relational data warehouse model consisting of two main entities with a defined relationship:

- **WarehouseEntity:** Represents the storage locations (ID, name, address, city, country).

- **ProductEntity:** Represents the stock items (UUID, name, category, quantity, unit).

- **Relationship:** A **One-to-Many** (`@OneToMany`) association, where one warehouse can contain a list of multiple products.

#### Required Program Adaptations

To ensure the system functions correctly, the following components were configured:

- **Persistence Layer:** JPA annotations (`@Entity`, `@Table`) were used to define the schema directly in Java.

- **Validation:** Jakarta Validation constraints (`@NotBlank`, `@Size`, `@Min`) were added to ensure data integrity.

- **ID Strategy:** Used `GenerationType.IDENTITY` for warehouses and `GenerationType.UUID` for products to ensure global uniqueness.

![](C:\Users\aronh\AppData\Roaming\marktext\images\2026-03-22-21-47-04-image.png)

## Extended Requirements

### CrudRepository Methods for Data Collection

The `CrudRepository` interface provides several methods to retrieve or "collect" data from the database:

| Method                          | Description                                              |
| ------------------------------- | -------------------------------------------------------- |
| `count()`                       | Returns the total number of entities available.          |
| `existsById(ID id)`             | Checks if an entity with the given ID exists.            |
| `findAll()`                     | Returns all instances of the entity type.                |
| `findAllById(Iterable<ID> ids)` | Returns all instances of the type with the provided IDs. |
| `findById(ID id)`               | Retrieves a single entity by its primary key.            |

#### **WarehouseRepository.java**

Added a custom JPQL query to retrieve a specific product associated with a specific warehouse. This was necessary because the relationship is unidirectional from Warehouse to Product.

```java
@Query("SELECT p FROM WarehouseEntity w JOIN w.products p WHERE w.id = :warehouseId AND p.id = :productId")
Optional<ProductEntity> findProductByWarehouseIdAndProductId(@Param("warehouseId") Long warehouseId, @Param("productId") UUID productId);
```

Extended the controller with new REST endpoints:

- **`GET /api/v1/warehouse/{id}`**: (Existing) Collects all data of one warehouse, including its products.
- **`GET /api/v1/warehouse/{warehouseId}/product/{productId}`**: (New) Collects a single product from a specific warehouse.
- **`PUT /api/v1/warehouse/{id}`**: (New) Updates an existing warehouse's details using its ID.
- **Validation**: Added `@Valid` to `@RequestBody` parameters to ensure data integrity during creation and updates.

#### **ProductPurchase Extension**

The data model was extended to track customer purchases, linking products and warehouses with transaction data:

- **`ProductPurchase.java`**: New JPA entity with `@ManyToOne` relationships to both `ProductEntity` and `WarehouseEntity`. It tracks `amount` and uses `@CreationTimestamp` for the transaction time.
- **`ProductPurchaseRepository.java`**: Repository with custom finders to filter purchases by `warehouseId` or `productId`.
- **`ProductPurchaseController.java`**: REST controller for creating and retrieving purchase records (`/api/v1/purchase`).

## Deep extension

``` sql
-- Use a temporary procedure to check if data already exists to prevent duplicate key errors on every start
DROP PROCEDURE IF EXISTS init_data;
DELIMITER //
CREATE PROCEDURE init_data()
BEGIN
    IF (SELECT COUNT(*) FROM warehouse) = 0 THEN
        -- 1. Insert 8 Austrian Warehouses
        INSERT INTO warehouse (name, address, postal_code, city, country, timestamp) VALUES 
        ('Zentrallager Wien', 'Handelskai 265', '1020', 'Wien', 'Österreich', NOW()),
        ('Logistikzentrum Linz', 'Industriezeile 35', '4020', 'Linz', 'Österreich', NOW()),
        ('Hub Graz', 'Puntigamer Straße 120', '8055', 'Graz', 'Österreich', NOW()),
        ('Lager Salzburg', 'Kasernenstraße 1', '5020', 'Salzburg', 'Österreich', NOW()),
        ('Innsbruck West', 'Innrain 52', '6020', 'Innsbruck', 'Österreich', NOW()),
        ('Klagenfurt Süd', 'Völkermarkter Straße 200', '9020', 'Klagenfurt', 'Österreich', NOW()),
        ('Dornbirn Nord', 'Messestraße 2', '6850', 'Dornbirn', 'Österreich', NOW()),
        ('St. Pölten Ost', 'Stattersdorfer Hauptstraße 30', '3100', 'St. Pölten', 'Österreich', NOW());

        -- 2. Insert 250 German Products
        INSERT INTO product (id, name, category, quantity, unit) VALUES 
        (UUID(), 'Apfel Bio 1', 'Obst & Gemüse', 1200, 'kg'), (UUID(), 'Banane Premium 2', 'Obst & Gemüse', 800, 'kg'),
        (UUID(), 'Milch Bio 3', 'Milchprodukte', 2500, 'Flasche'), (UUID(), 'Brot Bio 4', 'Backwaren', 150, 'Stück'),
        (UUID(), 'Käse Premium 5', 'Milchprodukte', 300, 'Packung'), (UUID(), 'Wurst Bio 6', 'Fleisch', 500, 'Packung'),
        (UUID(), 'Saft Bio 7', 'Getränke', 1000, 'Flasche'), (UUID(), 'Wasser Premium 8', 'Getränke', 4000, 'Flasche'),
        (UUID(), 'Ei Bio 9', 'Milchprodukte', 2000, 'Stück'), (UUID(), 'Butter Premium 10', 'Milchprodukte', 600, 'Packung'),
        (UUID(), 'Joghurt Bio 11', 'Milchprodukte', 1500, 'Becher'), (UUID(), 'Nudeln Premium 12', 'Backwaren', 3000, 'Packung'),
        (UUID(), 'Reis Bio 13', 'Backwaren', 2500, 'Packung'), (UUID(), 'Tomaten Bio 14', 'Obst & Gemüse', 900, 'kg'),
        (UUID(), 'Gurken Bio 15', 'Obst & Gemüse', 700, 'Stück'), (UUID(), 'Salat Bio 16', 'Obst & Gemüse', 400, 'Stück'),
        (UUID(), 'Zwiebeln Bio 17', 'Obst & Gemüse', 1100, 'kg'), (UUID(), 'Kartoffeln Bio 18', 'Obst & Gemüse', 5000, 'kg'),
        (UUID(), 'Fleischkäse Premium 19', 'Fleisch', 200, 'kg'), (UUID(), 'Speck Bio 20', 'Fleisch', 150, 'kg'),
        (UUID(), 'Apfel Premium 21', 'Obst & Gemüse', 1100, 'kg'), (UUID(), 'Banane Bio 22', 'Obst & Gemüse', 850, 'kg'),
        (UUID(), 'Milch Premium 23', 'Milchprodukte', 2400, 'Flasche'), (UUID(), 'Brot Premium 24', 'Backwaren', 160, 'Stück'),
        (UUID(), 'Käse Bio 25', 'Milchprodukte', 310, 'Packung'), (UUID(), 'Wurst Premium 26', 'Fleisch', 510, 'Packung'),
        (UUID(), 'Saft Premium 27', 'Getränke', 1010, 'Flasche'), (UUID(), 'Wasser Bio 28', 'Getränke', 4010, 'Flasche'),
        (UUID(), 'Ei Premium 29', 'Milchprodukte', 2010, 'Stück'), (UUID(), 'Butter Bio 30', 'Milchprodukte', 610, 'Packung'),
        (UUID(), 'Joghurt Premium 31', 'Milchprodukte', 1510, 'Becher'), (UUID(), 'Nudeln Bio 32', 'Backwaren', 3010, 'Packung'),
        (UUID(), 'Reis Premium 33', 'Backwaren', 2510, 'Packung'), (UUID(), 'Tomaten Premium 34', 'Obst & Gemüse', 910, 'kg'),
        (UUID(), 'Gurken Premium 35', 'Obst & Gemüse', 710, 'Stück'), (UUID(), 'Salat Premium 36', 'Obst & Gemüse', 410, 'Stück'),
        (UUID(), 'Zwiebeln Premium 37', 'Obst & Gemüse', 1110, 'kg'), (UUID(), 'Kartoffeln Premium 38', 'Obst & Gemüse', 5010, 'kg'),
        (UUID(), 'Fleischkäse Bio 39', 'Fleisch', 210, 'kg'), (UUID(), 'Speck Premium 40', 'Fleisch', 160, 'kg'),
        (UUID(), 'Apfel Bio 41', 'Obst & Gemüse', 1210, 'kg'), (UUID(), 'Banane Premium 42', 'Obst & Gemüse', 810, 'kg'),
        (UUID(), 'Milch Bio 43', 'Milchprodukte', 2510, 'Flasche'), (UUID(), 'Brot Bio 44', 'Backwaren', 170, 'Stück'),
        (UUID(), 'Käse Premium 45', 'Milchprodukte', 320, 'Packung'), (UUID(), 'Wurst Bio 46', 'Fleisch', 520, 'Packung'),
        (UUID(), 'Saft Bio 47', 'Getränke', 1020, 'Flasche'), (UUID(), 'Wasser Premium 48', 'Getränke', 4020, 'Flasche'),
        (UUID(), 'Ei Bio 49', 'Milchprodukte', 2020, 'Stück'), (UUID(), 'Butter Premium 50', 'Milchprodukte', 620, 'Packung'),
        (UUID(), 'Joghurt Bio 51', 'Milchprodukte', 1520, 'Becher'), (UUID(), 'Nudeln Premium 52', 'Backwaren', 3020, 'Packung'),
        (UUID(), 'Reis Bio 53', 'Backwaren', 2520, 'Packung'), (UUID(), 'Tomaten Bio 54', 'Obst & Gemüse', 920, 'kg'),
        (UUID(), 'Gurken Bio 55', 'Obst & Gemüse', 720, 'Stück'), (UUID(), 'Salat Bio 56', 'Obst & Gemüse', 420, 'Stück'),
        (UUID(), 'Zwiebeln Bio 57', 'Obst & Gemüse', 1120, 'kg'), (UUID(), 'Kartoffeln Bio 58', 'Obst & Gemüse', 5020, 'kg'),
        (UUID(), 'Fleischkäse Premium 59', 'Fleisch', 220, 'kg'), (UUID(), 'Speck Bio 60', 'Fleisch', 170, 'kg'),
        (UUID(), 'Apfel Premium 61', 'Obst & Gemüse', 1120, 'kg'), (UUID(), 'Banane Bio 62', 'Obst & Gemüse', 860, 'kg'),
        (UUID(), 'Milch Premium 63', 'Milchprodukte', 2420, 'Flasche'), (UUID(), 'Brot Premium 64', 'Backwaren', 180, 'Stück'),
        (UUID(), 'Käse Bio 65', 'Milchprodukte', 330, 'Packung'), (UUID(), 'Wurst Premium 66', 'Fleisch', 530, 'Packung'),
        (UUID(), 'Saft Premium 67', 'Getränke', 1030, 'Flasche'), (UUID(), 'Wasser Bio 68', 'Getränke', 4030, 'Flasche'),
        (UUID(), 'Ei Premium 69', 'Milchprodukte', 2030, 'Stück'), (UUID(), 'Butter Bio 70', 'Milchprodukte', 630, 'Packung'),
        (UUID(), 'Joghurt Premium 71', 'Milchprodukte', 1530, 'Becher'), (UUID(), 'Nudeln Bio 72', 'Backwaren', 3030, 'Packung'),
        (UUID(), 'Reis Premium 73', 'Backwaren', 2530, 'Packung'), (UUID(), 'Tomaten Premium 74', 'Obst & Gemüse', 930, 'kg'),
        (UUID(), 'Gurken Premium 75', 'Obst & Gemüse', 730, 'Stück'), (UUID(), 'Salat Premium 76', 'Obst & Gemüse', 430, 'Stück'),
        (UUID(), 'Zwiebeln Premium 77', 'Obst & Gemüse', 1130, 'kg'), (UUID(), 'Kartoffeln Premium 78', 'Obst & Gemüse', 5030, 'kg'),
        (UUID(), 'Fleischkäse Bio 79', 'Fleisch', 230, 'kg'), (UUID(), 'Speck Premium 80', 'Fleisch', 180, 'kg'),
        (UUID(), 'Apfel Bio 81', 'Obst & Gemüse', 1230, 'kg'), (UUID(), 'Banane Premium 82', 'Obst & Gemüse', 830, 'kg'),
        (UUID(), 'Milch Bio 83', 'Milchprodukte', 2530, 'Flasche'), (UUID(), 'Brot Bio 84', 'Backwaren', 190, 'Stück'),
        (UUID(), 'Käse Premium 85', 'Milchprodukte', 340, 'Packung'), (UUID(), 'Wurst Bio 86', 'Fleisch', 540, 'Packung'),
        (UUID(), 'Saft Bio 87', 'Getränke', 1040, 'Flasche'), (UUID(), 'Wasser Premium 88', 'Getränke', 4040, 'Flasche'),
        (UUID(), 'Ei Bio 89', 'Milchprodukte', 2040, 'Stück'), (UUID(), 'Butter Premium 90', 'Milchprodukte', 640, 'Packung'),
        (UUID(), 'Joghurt Bio 91', 'Milchprodukte', 1540, 'Becher'), (UUID(), 'Nudeln Premium 92', 'Backwaren', 3040, 'Packung'),
        (UUID(), 'Reis Bio 93', 'Backwaren', 2540, 'Packung'), (UUID(), 'Tomaten Bio 94', 'Obst & Gemüse', 940, 'kg'),
        (UUID(), 'Gurken Bio 95', 'Obst & Gemüse', 740, 'Stück'), (UUID(), 'Salat Bio 96', 'Obst & Gemüse', 440, 'Stück'),
        (UUID(), 'Zwiebeln Bio 97', 'Obst & Gemüse', 1140, 'kg'), (UUID(), 'Kartoffeln Bio 98', 'Obst & Gemüse', 5040, 'kg'),
        (UUID(), 'Fleischkäse Premium 99', 'Fleisch', 240, 'kg'), (UUID(), 'Speck Bio 100', 'Fleisch', 190, 'kg'),
        (UUID(), 'Kaffee Bio 101', 'Getränke', 500, 'Packung'), (UUID(), 'Tee Bio 102', 'Getränke', 300, 'Packung'),
        (UUID(), 'Honig Bio 103', 'Brotaufstrich', 200, 'Glas'), (UUID(), 'Marmelade Bio 104', 'Brotaufstrich', 400, 'Glas'),
        (UUID(), 'Schokolade Premium 105', 'Süßwaren', 1000, 'Tafel'), (UUID(), 'Gummibärchen Bio 106', 'Süßwaren', 800, 'Packung'),
        (UUID(), 'Chips Premium 107', 'Snacks', 600, 'Packung'), (UUID(), 'Salzstangen Bio 108', 'Snacks', 400, 'Packung'),
        (UUID(), 'Mehl Bio 109', 'Backzutaten', 2000, 'kg'), (UUID(), 'Zucker Bio 110', 'Backzutaten', 1500, 'kg'),
        (UUID(), 'Salz Premium 111', 'Gewürze', 1000, 'Packung'), (UUID(), 'Pfeffer Bio 112', 'Gewürze', 200, 'Glas'),
        (UUID(), 'Öl Bio 113', 'Fette & Öle', 500, 'Flasche'), (UUID(), 'Essig Bio 114', 'Fette & Öle', 400, 'Flasche'),
        (UUID(), 'Senf Premium 115', 'Saucen', 300, 'Tube'), (UUID(), 'Ketchup Bio 116', 'Saucen', 600, 'Flasche'),
        (UUID(), 'Mayonnaise Premium 117', 'Saucen', 400, 'Glas'), (UUID(), 'Nudelsauce Bio 118', 'Fertiggerichte', 800, 'Glas'),
        (UUID(), 'Suppe Bio 119', 'Fertiggerichte', 1000, 'Dose'), (UUID(), 'Pizza TK Premium 120', 'Tiefkühlkost', 500, 'Stück'),
        (UUID(), 'Eis Bio 121', 'Tiefkühlkost', 300, 'Becher'), (UUID(), 'Erbsen TK Bio 122', 'Tiefkühlkost', 600, 'Packung'),
        (UUID(), 'Spinat TK Bio 123', 'Tiefkühlkost', 400, 'Packung'), (UUID(), 'Fischstäbchen Premium 124', 'Tiefkühlkost', 200, 'Packung'),
        (UUID(), 'Hähnchen Bio 125', 'Fleisch', 100, 'Stück'), (UUID(), 'Rindfleisch Bio 126', 'Fleisch', 50, 'kg'),
        (UUID(), 'Schweinefleisch Bio 127', 'Fleisch', 80, 'kg'), (UUID(), 'Lachs Premium 128', 'Fisch', 30, 'kg'),
        (UUID(), 'Forelle Bio 129', 'Fisch', 40, 'Stück'), (UUID(), 'Garnelen Premium 130', 'Fisch', 20, 'Packung'),
        (UUID(), 'Paprika Bio 131', 'Obst & Gemüse', 500, 'kg'), (UUID(), 'Brokkoli Bio 132', 'Obst & Gemüse', 300, 'Stück'),
        (UUID(), 'Blumenkohl Bio 133', 'Obst & Gemüse', 200, 'Stück'), (UUID(), 'Karotten Bio 134', 'Obst & Gemüse', 1000, 'kg'),
        (UUID(), 'Spargel Premium 135', 'Obst & Gemüse', 100, 'kg'), (UUID(), 'Erdbeeren Bio 136', 'Obst & Gemüse', 200, 'Schale'),
        (UUID(), 'Himbeeren Bio 137', 'Obst & Gemüse', 150, 'Schale'), (UUID(), 'Blaubeeren Bio 138', 'Obst & Gemüse', 100, 'Schale'),
        (UUID(), 'Zitrone Bio 139', 'Obst & Gemüse', 400, 'Stück'), (UUID(), 'Orange Bio 140', 'Obst & Gemüse', 600, 'kg'),
        (UUID(), 'Kiwi Bio 141', 'Obst & Gemüse', 300, 'Stück'), (UUID(), 'Mango Premium 142', 'Obst & Gemüse', 100, 'Stück'),
        (UUID(), 'Ananas Bio 143', 'Obst & Gemüse', 50, 'Stück'), (UUID(), 'Melone Bio 144', 'Obst & Gemüse', 40, 'Stück'),
        (UUID(), 'Trauben Bio 145', 'Obst & Gemüse', 300, 'kg'), (UUID(), 'Birne Bio 146', 'Obst & Gemüse', 500, 'kg'),
        (UUID(), 'Pflaume Bio 147', 'Obst & Gemüse', 200, 'kg'), (UUID(), 'Kirsche Bio 148', 'Obst & Gemüse', 100, 'kg'),
        (UUID(), 'Pfirsich Bio 149', 'Obst & Gemüse', 150, 'kg'), (UUID(), 'Nektarine Bio 150', 'Obst & Gemüse', 150, 'kg'),
        (UUID(), 'Knoblauch Bio 151', 'Obst & Gemüse', 200, 'Netz'), (UUID(), 'Ingwer Bio 152', 'Obst & Gemüse', 100, 'kg'),
        (UUID(), 'Chili Bio 153', 'Obst & Gemüse', 50, 'Packung'), (UUID(), 'Kräuter Bio 154', 'Obst & Gemüse', 300, 'Topf'),
        (UUID(), 'Pilze Bio 155', 'Obst & Gemüse', 200, 'Schale'), (UUID(), 'Lauch Bio 156', 'Obst & Gemüse', 100, 'Stück'),
        (UUID(), 'Sellerie Bio 157', 'Obst & Gemüse', 80, 'Stück'), (UUID(), 'Rote Bete Bio 158', 'Obst & Gemüse', 150, 'kg'),
        (UUID(), 'Kürbis Bio 159', 'Obst & Gemüse', 40, 'Stück'), (UUID(), 'Zucchini Bio 160', 'Obst & Gemüse', 200, 'kg'),
        (UUID(), 'Aubergine Bio 161', 'Obst & Gemüse', 100, 'Stück'), (UUID(), 'Mais Bio 162', 'Obst & Gemüse', 50, 'Dose'),
        (UUID(), 'Bohnen Bio 163', 'Obst & Gemüse', 300, 'Dose'), (UUID(), 'Linsen Bio 164', 'Obst & Gemüse', 200, 'Packung'),
        (UUID(), 'Kichererbsen Bio 165', 'Obst & Gemüse', 150, 'Dose'), (UUID(), 'Quinoa Bio 166', 'Getreide', 100, 'Packung'),
        (UUID(), 'Couscous Bio 167', 'Getreide', 150, 'Packung'), (UUID(), 'Bulgur Bio 168', 'Getreide', 100, 'Packung'),
        (UUID(), 'Haferflocken Bio 169', 'Getreide', 500, 'Packung'), (UUID(), 'Müsli Bio 170', 'Getreide', 400, 'Packung'),
        (UUID(), 'Vollkornbrot Bio 171', 'Backwaren', 100, 'Stück'), (UUID(), 'Brötchen Bio 172', 'Backwaren', 500, 'Stück'),
        (UUID(), 'Brezel Bio 173', 'Backwaren', 200, 'Stück'), (UUID(), 'Croissant Bio 174', 'Backwaren', 150, 'Stück'),
        (UUID(), 'Kuchen Bio 175', 'Backwaren', 20, 'Stück'), (UUID(), 'Kekse Bio 176', 'Süßwaren', 300, 'Packung'),
        (UUID(), 'Gouda Bio 177', 'Milchprodukte', 100, 'Packung'), (UUID(), 'Emmentaler Bio 178', 'Milchprodukte', 80, 'Packung'),
        (UUID(), 'Mozzarella Bio 179', 'Milchprodukte', 150, 'Becher'), (UUID(), 'Feta Bio 180', 'Milchprodukte', 100, 'Packung'),
        (UUID(), 'Frischkäse Bio 181', 'Milchprodukte', 200, 'Becher'), (UUID(), 'Quark Bio 182', 'Milchprodukte', 300, 'Becher'),
        (UUID(), 'Sahne Bio 183', 'Milchprodukte', 400, 'Becher'), (UUID(), 'Saure Sahne Bio 184', 'Milchprodukte', 200, 'Becher'),
        (UUID(), 'Schmand Bio 185', 'Milchprodukte', 150, 'Becher'), (UUID(), 'Creme Fraiche Bio 186', 'Milchprodukte', 100, 'Becher'),
        (UUID(), 'H-Milch Bio 187', 'Milchprodukte', 1000, 'Packung'), (UUID(), 'Kondensmilch Bio 188', 'Milchprodukte', 200, 'Dose'),
        (UUID(), 'Sojadrink Bio 189', 'Milchprodukte', 300, 'Packung'), (UUID(), 'Haferdrink Bio 190', 'Milchprodukte', 400, 'Packung'),
        (UUID(), 'Mandeldrink Bio 191', 'Milchprodukte', 200, 'Packung'), (UUID(), 'Reisdrink Bio 192', 'Milchprodukte', 150, 'Packung'),
        (UUID(), 'Tofu Bio 193', 'Fleischersatz', 100, 'Packung'), (UUID(), 'Seitan Bio 194', 'Fleischersatz', 50, 'Packung'),
        (UUID(), 'Tempeh Bio 195', 'Fleischersatz', 30, 'Packung'), (UUID(), 'Veggie-Wurst Bio 196', 'Fleischersatz', 100, 'Packung'),
        (UUID(), 'Veggie-Burger Bio 197', 'Fleischersatz', 80, 'Packung'), (UUID(), 'Veggie-Hack Bio 198', 'Fleischersatz', 50, 'Packung'),
        (UUID(), 'Eier Bio 199', 'Eier', 500, 'Packung'), (UUID(), 'Wachteleier Bio 200', 'Eier', 20, 'Packung'),
        (UUID(), 'Senf Bio 201', 'Saucen', 100, 'Glas'), (UUID(), 'Meerrettich Bio 202', 'Saucen', 50, 'Glas'),
        (UUID(), 'Remoulade Bio 203', 'Saucen', 80, 'Tube'), (UUID(), 'Pesto Bio 204', 'Saucen', 150, 'Glas'),
        (UUID(), 'Oliven Bio 205', 'Feinkost', 200, 'Glas'), (UUID(), 'Kapern Bio 206', 'Feinkost', 50, 'Glas'),
        (UUID(), 'Artischocken Bio 207', 'Feinkost', 30, 'Glas'), (UUID(), 'Getrocknete Tomaten Bio 208', 'Feinkost', 100, 'Glas'),
        (UUID(), 'Walnüsse Bio 209', 'Nüsse', 200, 'Packung'), (UUID(), 'Haselnüsse Bio 210', 'Nüsse', 150, 'Packung'),
        (UUID(), 'Mandeln Bio 211', 'Nüsse', 300, 'Packung'), (UUID(), 'Cashews Bio 212', 'Nüsse', 250, 'Packung'),
        (UUID(), 'Erdnüsse Bio 213', 'Nüsse', 400, 'Packung'), (UUID(), 'Pistazien Bio 214', 'Nüsse', 100, 'Packung'),
        (UUID(), 'Sonnenblumenkerne Bio 215', 'Kerne', 500, 'Packung'), (UUID(), 'Kürbiskerne Bio 216', 'Kerne', 300, 'Packung'),
        (UUID(), 'Leinsamen Bio 217', 'Kerne', 400, 'Packung'), (UUID(), 'Chia-Samen Bio 218', 'Kerne', 200, 'Packung'),
        (UUID(), 'Trockenpflaumen Bio 219', 'Trockenobst', 150, 'Packung'), (UUID(), 'Rosinen Bio 220', 'Trockenobst', 300, 'Packung'),
        (UUID(), 'Datteln Bio 221', 'Trockenobst', 100, 'Packung'), (UUID(), 'Feigen Bio 222', 'Trockenobst', 80, 'Packung'),
        (UUID(), 'Aprikosen getrocknet Bio 223', 'Trockenobst', 120, 'Packung'), (UUID(), 'Mango getrocknet Bio 224', 'Trockenobst', 50, 'Packung'),
        (UUID(), 'Bier Bio 225', 'Getränke', 1000, 'Flasche'), (UUID(), 'Wein Bio 226', 'Getränke', 200, 'Flasche'),
        (UUID(), 'Sekt Bio 227', 'Getränke', 50, 'Flasche'), (UUID(), 'Limonade Bio 228', 'Getränke', 600, 'Flasche'),
        (UUID(), 'Eistee Bio 229', 'Getränke', 400, 'Packung'), (UUID(), 'Mineralwasser Bio 230', 'Getränke', 2000, 'Flasche'),
        (UUID(), 'Apfelsaft Bio 231', 'Getränke', 800, 'Packung'), (UUID(), 'Orangensaft Bio 232', 'Getränke', 700, 'Packung'),
        (UUID(), 'Traubensaft Bio 233', 'Getränke', 300, 'Flasche'), (UUID(), 'Multivitaminsaft Bio 234', 'Getränke', 500, 'Packung'),
        (UUID(), 'Tomatensaft Bio 235', 'Getränke', 100, 'Packung'), (UUID(), 'Gemüsesaft Bio 236', 'Getränke', 150, 'Packung'),
        (UUID(), 'Cola Bio 237', 'Getränke', 300, 'Flasche'), (UUID(), 'Fruchtschorle Bio 238', 'Getränke', 500, 'Flasche'),
        (UUID(), 'Malzbier Bio 239', 'Getränke', 200, 'Flasche'), (UUID(), 'Energydrink Bio 240', 'Getränke', 100, 'Dose'),
        (UUID(), 'Kräutertee Bio 241', 'Getränke', 300, 'Packung'), (UUID(), 'Früchtetee Bio 242', 'Getränke', 250, 'Packung'),
        (UUID(), 'Schwarzer Tee Bio 243', 'Getränke', 200, 'Packung'), (UUID(), 'Grüner Tee Bio 244', 'Getränke', 150, 'Packung'),
        (UUID(), 'Rooibos Tee Bio 245', 'Getränke', 100, 'Packung'), (UUID(), 'Instant-Kaffee Bio 246', 'Getränke', 50, 'Glas'),
        (UUID(), 'Kaffeebohnen Bio 247', 'Getränke', 400, 'Packung'), (UUID(), 'Espresso Bio 248', 'Getränke', 200, 'Packung'),
        (UUID(), 'Kakao Bio 249', 'Getränke', 300, 'Packung'), (UUID(), 'Trinkschokolade Bio 250', 'Getränke', 150, 'Dose');

        -- 3. Insert 300 Purchase Records
        INSERT INTO product_purchase (product_id, warehouse_id, amount, timestamp)
        SELECT p.id, w.id, (FLOOR(1 + RAND() * 50)), (NOW() - INTERVAL FLOOR(RAND() * 365) DAY)
        FROM product p
        CROSS JOIN (SELECT id FROM warehouse) w
        ORDER BY RAND()
        LIMIT 300;
    END IF;
END //
DELIMITER ;
CALL init_data();
DROP PROCEDURE init_data;

```

![](C:\Users\aronh\AppData\Roaming\marktext\images\2026-03-23-18-41-10-image.png)

Here is a `init.sql` for the program that initializes many Purchases, Products and Warehouses for the LLM that generate a preview of sales numbers for all warehouses and products over the coming months

### 1. Automated Data Initialization (`DataLoader`)

Instead of manual data entry, a robust **initialization logic** was implemented to simulate a real-world environment.

- **Dynamic Generation:** Upon system startup, the `DataLoader` checks the database status. If empty, it automatically generates **8 Austrian warehouses** (e.g., Vienna, Linz, Graz) and **250 products**.

- **Randomized Test Data:** Products are assigned randomized categories, units, and quality tiers (e.g., "Bio", "Premium", "Budget").

- **Transaction Simulation:** The system generates **300 historical purchase records** (`ProductPurchase`) spread across the last year. This provides the essential data foundation for subsequent AI analysis.

### 2. Implementation of the Sales History Model

The data model was expanded beyond static inventory to include a transaction-based entity: `ProductPurchase`.

- **Relational Link:** This entity acts as a bridge between products and warehouses.

- **Data Points:** It captures which product was sold at which location, the specific quantity (`amount`), and the exact time of the transaction (`timestamp`).

### 3. AI-Powered Sales Forecasting (`ForecastService`)

This is the core "Deep Dive" functional extension. The system now utilizes **Local AI (Ollama)** to support business intelligence and decision-making.

- **Data Aggregation:** The service fetches all historical sales from the database and groups them by product name and warehouse location using Java Streams.

- **Prompt Engineering:** It automatically constructs a detailed prompt for the AI, containing the aggregated sales figures.

- **Local AI Integration:** Using a `RestTemplate` interface, Spring Boot communicates with a local **Ollama** server (using models like `qwen3`).

- **Predictive Analysis:** The AI analyzes the historical trends and returns a **3-month sales forecast** and a detailed trend analysis.

### 4. REST API for Real-Time Forecasts

A new API endpoint was created to make the AI analysis accessible:

- **Endpoint:** `GET /api/v1/forecast`

- **Functionality:** Triggers the AI analysis in real-time and delivers the forecast as a text response to the user or a frontend application.
