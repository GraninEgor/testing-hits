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
        category: document.getElementById("p-category").value,
        cookingRequirement: document.getElementById("p-cooking").value,
        composition: null,
        flags: []
    };

    // 🔥 ВАЖНО: не теряем фото
    dto.photos =
        fileInput.files.length > 0
            ? [] // сервер перезапишет через file
            : (existingPhoto ? [existingPhoto] : []);

    if (!validateBJU(dto)) return;

    const formData = new FormData();
    formData.append(
        "data",
        new Blob([JSON.stringify(dto)], { type: "application/json" })
    );

    if (fileInput.files.length > 0) {
        formData.append("file", fileInput.files[0]);
    }

    if (editingProductId) {
        await fetch(`${PRODUCTS_API}/${editingProductId}`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(dto)
        });
        editingProductId = null;
        existingPhoto = null;
    } else {
        await fetch(PRODUCTS_API, {
            method: "POST",
        });
    }

    document.getElementById("product-form").reset();
    document.getElementById("preview").classList.add("hidden");

    loadProducts();
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

    const ingredientIds = [];

    document.querySelectorAll(".ingredient-product").forEach(select => {
        ingredientIds.push(+select.value);
    });

    const dto = {
        name: document.getElementById("d-name").value,
        photos: [],
        calories: +document.getElementById("d-calories").value,
        proteins: +document.getElementById("d-proteins").value,
        fats: +document.getElementById("d-fats").value,
        carbohydrates: +document.getElementById("d-carbs").value,
        ingredientIds,
        portionSize: +document.getElementById("d-portion").value,
        category: "SECOND",
        flags: []
    };

    await fetch(DISHES_API, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(dto)
    });

    loadDishes();
}

/* =========================
   LOAD DISHES
========================= */

async function loadDishes() {
    const response = await fetch(DISHES_API);
    const data = await response.json();

    const list = document.getElementById("dishes-list");
    list.innerHTML = "";

    const dishes = data.content || data;

    dishes.forEach(d => {
        const card = document.createElement("div");
        card.className = "card";

        card.innerHTML = `
            <b>${d.name}</b><br>
            Ккал: ${d.calories ?? "-"}
        `;

        list.appendChild(card);
    });
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

/* =========================
   INIT
========================= */

loadProducts();
loadDishes();