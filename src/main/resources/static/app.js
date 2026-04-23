const PRODUCTS_API = "/rest/admin-ui/products";
const DISHES_API = "/rest/admin-ui/dishes";

let allProducts = [];
let editingProductId = null;
let existingPhoto = null;

/* =========================
   ТАБЫ
========================= */

function showTab(tab) {
    document.getElementById("products-section").classList.add("hidden");
    document.getElementById("dishes-section").classList.add("hidden");
    document.getElementById("product-detail-section").classList.add("hidden");

    document.getElementById(tab + "-section").classList.remove("hidden");
}

function extractProducts(data) {
    // обычный массив
    if (Array.isArray(data)) return data;

    // Spring Page
    if (Array.isArray(data.content)) return data.content;

    // Spring HATEOAS (PagedModel)
    if (data._embedded) {
        const firstKey = Object.keys(data._embedded)[0];
        return data._embedded[firstKey] || [];
    }

    return [];
}

async function startEditProduct(id) {
    const res = await fetch(`${PRODUCTS_API}/${id}`);
    const p = await res.json();

    editingProductId = id;
    existingPhoto = p.photos?.[0] || null;

    document.getElementById("p-name").value = p.name ?? "";
    document.getElementById("p-calories").value = p.calories ?? 0;
    document.getElementById("p-proteins").value = p.proteins ?? 0;
    document.getElementById("p-fats").value = p.fats ?? 0;
    document.getElementById("p-carbs").value = p.carbohydrates ?? 0;
    document.getElementById("p-category").value = p.category ?? "";
    document.getElementById("p-cooking").value = p.cookingRequirement ?? "";

    // сброс
    document.querySelectorAll(".p-flag").forEach(cb => cb.checked = false);

// установка
    p.flags?.forEach(flag => {
        const el = document.querySelector(`.p-flag[value="${flag}"]`);
        if (el) el.checked = true;
    });

    const preview = document.getElementById("preview");


    if (existingPhoto) {
        preview.src = existingPhoto;
        preview.classList.remove("hidden");
    } else {
        preview.classList.add("hidden");
    }
}

function validateBJU(dto) {
    const sum = dto.proteins + dto.fats + dto.carbohydrates;

    if (sum > 100) {
        alert("Сумма БЖУ на 100г не может превышать 100");
        return false;
    }

    return true;
}

async function saveProduct() {
    const fileInput = document.getElementById("p-photo");

    const dto = {
        name: document.getElementById("p-name").value,
        calories: +document.getElementById("p-calories").value,
        proteins: +document.getElementById("p-proteins").value,
        fats: +document.getElementById("p-fats").value,
        carbohydrates: +document.getElementById("p-carbs").value,
        composition: null,
        category: document.getElementById("p-category").value,
        cookingRequirement: document.getElementById("p-cooking").value,
        flags: getProductFlags(),
        photos: []
    };

    const formData = new FormData();
    formData.append(
        "data",
        new Blob([JSON.stringify(dto)], { type: "application/json" })
    );

    if (fileInput.files.length > 0) {
        formData.append("file", fileInput.files[0]);
    }

    if (editingProductId) {
        // UPDATE
        await fetch(`${PRODUCTS_API}/${editingProductId}`, {
            method: "PATCH",
            body: formData
        });

        editingProductId = null;
        existingPhoto = null;

    } else {
        // CREATE 👇 ВОТ ЭТОГО У ТЕБЯ НЕ БЫЛО
        await fetch(PRODUCTS_API, {
            method: "POST",
            body: formData
        });
    }

    document.getElementById("product-form").reset();
    document.getElementById("preview").classList.add("hidden");

    loadProducts();
}
async function uploadPhoto(productId, file) {
    const formData = new FormData();
    formData.append("file", file);

    await fetch(`${PRODUCTS_API}/${productId}/photo`, {
        method: "PATCH",
        body: formData
    });
}
/* =========================
   PRODUCTS
========================= */

async function loadProducts() {

    const params = new URLSearchParams();

    const search = document.getElementById("product-search").value;
    const category = document.getElementById("filter-category").value;
    const cooking = document.getElementById("filter-cooking").value;
    const sort = document.getElementById("sort").value;

    if (search) params.append("search", search);
    if (category) params.append("category", category);
    if (cooking) params.append("cookingRequirement", cooking);

    if (sort) {
        const [field, dir] = sort.split(",");
        params.append("sort", `${field},${dir}`);
    }

    const response = await fetch(`${PRODUCTS_API}?${params.toString()}`);
    const data = await response.json();

    allProducts = extractProducts(data);
    renderProducts(allProducts);
}

function renderProducts(products) {
    const list = document.getElementById("products-list");
    list.innerHTML = "";

    products.forEach(p => {
        if (!p?.id) return;

        const card = document.createElement("div");
        card.className = "card";

        card.innerHTML = `
            ${p.photos?.[0] ? `<img src="${p.photos[0]}" width="100">` : ""}
            <b>${p.name}</b><br>
            Ккал: ${p.calories}<br>

            <button onclick="openProduct(${p.id})">Открыть</button>
            <button onclick="startEditProduct(${p.id})">Редактировать</button>
        `;

        list.appendChild(card);
    });
}
/* =========================
   PREVIEW IMAGE
========================= */

document.addEventListener("DOMContentLoaded", () => {
    const photoInput = document.getElementById("p-photo");

    if (photoInput) {
        photoInput.addEventListener("change", e => {
            const file = e.target.files[0];
            if (!file) return;

            const preview = document.getElementById("preview");
            preview.src = URL.createObjectURL(file);
            preview.classList.remove("hidden");
        });
    }
});

/* =========================
   CREATE PRODUCT (multipart)
========================= */

async function createProduct() {
    const fileInput = document.getElementById("p-photo");

    const dto = {
        name: document.getElementById("p-name").value,
        photos: [],
        calories: +document.getElementById("p-calories").value,
        proteins: +document.getElementById("p-proteins").value,
        fats: +document.getElementById("p-fats").value,
        carbohydrates: +document.getElementById("p-carbs").value,
        composition: null,
        category: document.getElementById("p-category").value,
        cookingRequirement: document.getElementById("p-cooking").value,
        flags: []
    };

    const formData = new FormData();

    formData.append(
        "data",
        new Blob([JSON.stringify(dto)], { type: "application/json" })
    );

    if (fileInput.files.length) {
        formData.append("file", fileInput.files[0]);
    }

    await fetch(PRODUCTS_API, {
        method: "POST",
        body: formData
    });

    loadProducts();
}

/* =========================
   INGREDIENTS
========================= */

function addIngredientRow() {
    const container = document.getElementById("ingredients");

    container.innerHTML += `
        <div class="ingredient-row">
            <select class="ingredient-product"></select>
            <input class="ingredient-amount" type="number" placeholder="граммы">
        </div>
    `;

    populateIngredientSelects();
}

function populateIngredientSelects() {
    const selects = document.querySelectorAll(".ingredient-product");

    selects.forEach(select => {
        select.innerHTML = "";

        allProducts.forEach(product => {
            select.innerHTML += `<option value="${product.id}">${product.name}</option>`;
        });
    });
}

/* =========================
   CALCULATE MACROS
========================= */

function calculateDishMacros() {
    let calories = 0;
    let proteins = 0;
    let fats = 0;
    let carbs = 0;

    const rows = document.querySelectorAll(".ingredient-row");

    rows.forEach(row => {
        const productId = row.querySelector(".ingredient-product").value;
        const amount = +row.querySelector(".ingredient-amount").value;

        const product = allProducts.find(p => p.id == productId);

        if (product) {
            calories += product.calories * amount / 100;
            proteins += product.proteins * amount / 100;
            fats += product.fats * amount / 100;
            carbs += product.carbohydrates * amount / 100;
        }
    });

    document.getElementById("d-calories").value = calories.toFixed(1);
    document.getElementById("d-proteins").value = proteins.toFixed(1);
    document.getElementById("d-fats").value = fats.toFixed(1);
    document.getElementById("d-carbs").value = carbs.toFixed(1);
}

/* =========================
   CREATE DISH (JSON FIXED)
========================= */

async function createDish() {

    const rawName = document.getElementById("d-name").value;
    const parsed = parseDishName(rawName);

    const ingredients = [];

    document.querySelectorAll(".ingredient-row").forEach(row => {
        const productId = +row.querySelector(".ingredient-product").value;
        const amount = +row.querySelector(".ingredient-amount").value;

        if (productId && amount > 0) {
            ingredients.push({
                productId: productId,
                amount: amount
            });
        }
    });

    if (ingredients.length === 0) {
        alert("Добавь хотя бы 1 ингредиент");
        return;
    }

    const manualCategory = document.getElementById("d-category")?.value;

    const dto = {
        name: parsed.cleanName,
        photos: [],
        calories: +document.getElementById("d-calories").value,
        proteins: +document.getElementById("d-proteins").value,
        fats: +document.getElementById("d-fats").value,
        carbohydrates: +document.getElementById("d-carbs").value,

        // ✅ ВАЖНО: теперь отправляем ingredients
        ingredients: ingredients,

        portionSize: +document.getElementById("d-portion").value,
        category: manualCategory || parsed.category || "SECOND",
        flags: getDishFlags()
    };

    if (!validateBJU(dto)) return;

    await fetch(DISHES_API, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(dto)
    });

    loadDishes();
}

function getProductFlags() {
    const flags = [];

    document.querySelectorAll(".p-flag:checked").forEach(cb => {
        flags.push(cb.value);
    });

    return flags;
}
/* =========================
   LOAD DISHES
========================= */

async function loadDishes() {

    const params = new URLSearchParams();

    const search = document.getElementById("dish-search")?.value;
    const category = document.getElementById("dish-category")?.value;

    if (search) params.append("search", search);
    if (category) params.append("category", category);

    const res = await fetch(`${DISHES_API}?${params.toString()}`);
    const data = await res.json();

    const dishes = data.content || data;

    const list = document.getElementById("dishes-list");
    list.innerHTML = "";

    dishes.forEach(d => {
        const card = document.createElement("div");

        card.className = "card";
        card.innerHTML = `
            <b>${d.name}</b><br>
            Ккал: ${d.calories ?? "-"}<br>

            <button onclick="openDish(${d.id})">Открыть</button>
            <button onclick="editDish(${d.id})">Редактировать</button>
            <button onclick="deleteDish(${d.id})">Удалить</button>
        `;

        list.appendChild(card);
    });
}

async function deleteDish(id) {
    await fetch(`${DISHES_API}/${id}`, {
        method: "DELETE"
    });

    loadDishes();
}


async function openDish(id) {
    const res = await fetch(`${DISHES_API}/${id}`);
    const d = await res.json();

    alert(`
${d.name}

Ккал: ${d.calories}
Б: ${d.proteins}
Ж: ${d.fats}
У: ${d.carbohydrates}

Порция: ${d.portionSize}
`);
}


async function openProduct(id) {
    if (!id) return;

    const res = await fetch(`${PRODUCTS_API}/${id}`);
    const p = await res.json();

    document.getElementById("products-section").classList.add("hidden");
    document.getElementById("dishes-section").classList.add("hidden");
    document.getElementById("product-detail-section").classList.remove("hidden");

    document.getElementById("product-detail").innerHTML = `
        <div class="card large">
            ${p.photos?.[0] ? `<img src="${p.photos[0]}" width="250">` : ""}

            <h2>${p.name ?? "-"}</h2>

            <p><b>Калории:</b> ${p.calories ?? "-"}</p>
            <p><b>Белки:</b> ${p.proteins ?? "-"}</p>
            <p><b>Жиры:</b> ${p.fats ?? "-"}</p>
            <p><b>Углеводы:</b> ${p.carbohydrates ?? "-"}</p>

            <p><b>Категория:</b> ${p.category ?? "-"}</p>
            <p><b>Готовка:</b> ${p.cookingRequirement ?? "-"}</p>

            <p><b>Состав:</b> ${p.composition ?? "-"}</p>

            <p><b>Флаги:</b> ${p.flags?.join(", ") || "-"}</p>
        </div>
    `;
}

function closeProductView() {
    document.getElementById("product-detail-section").classList.add("hidden");
    document.getElementById("products-section").classList.remove("hidden");
}

function parseDishName(name) {
    const macros = {
        "!десерт": "DESSERT",
        "!первое": "FIRST",
        "!второе": "SECOND",
        "!напиток": "DRINK",
        "!салат": "SALAD",
        "!суп": "SOUP",
        "!перекус": "SNACK"
    };

    for (const key in macros) {
        if (name.toLowerCase().includes(key)) {
            return {
                cleanName: name.replace(key, "").trim(),
                category: macros[key]
            };
        }
    }

    return { cleanName: name, category: null };
}

function getDishFlags() {
    const rows = document.querySelectorAll(".ingredient-row");

    let vegan = true;
    let glutenFree = true;
    let sugarFree = true;

    rows.forEach(row => {
        const productId = row.querySelector(".ingredient-product").value;
        const product = allProducts.find(p => p.id == productId);

        if (!product) return;

        if (!product.flags?.includes("VEGAN")) vegan = false;
        if (!product.flags?.includes("GLUTEN_FREE")) glutenFree = false;
        if (!product.flags?.includes("SUGAR_FREE")) sugarFree = false;
    });

    const flags = [];

    if (vegan) flags.push("VEGAN");
    if (glutenFree) flags.push("GLUTEN_FREE");
    if (sugarFree) flags.push("SUGAR_FREE");

    return flags;
}

document.addEventListener("input", e => {
    if (e.target.classList.contains("ingredient-amount") ||
        e.target.classList.contains("ingredient-product")) {
        calculateDishMacros();
    }
});




/* =========================
   INIT
========================= */

loadProducts();
loadDishes();