const PRODUCTS_API = "/rest/admin-ui/products";
const DISHES_API = "/rest/admin-ui/dishes";

/* =========================
   📍 ГЛОБАЛЬНОЕ СОСТОЯНИЕ
========================= */

let allProducts = [];
let editingProductId = null;
let existingPhoto = null;
let manualMacros = false;
let editDishId = null;
let productFiles = [];
let dishFiles = [];

/* =========================
   📍 ТАБЫ
========================= */

["d-calories", "d-proteins", "d-fats", "d-carbs"].forEach(id => {
    document.getElementById(id).addEventListener("input", () => {
        manualMacros = true;
    });
});

document.getElementById("p-photo")?.addEventListener("change", e => {
    const newFiles = Array.from(e.target.files);

    // 👉 добавляем, а не перезаписываем
    productFiles = [...productFiles, ...newFiles];

    renderProductPreview();

    // 👉 сбрасываем input чтобы можно было выбрать те же файлы снова
    e.target.value = "";
});


function renderProductPreview() {
    const container = document.getElementById("p-photo-preview");
    container.innerHTML = "";

    productFiles.forEach((file, index) => {
        const url = URL.createObjectURL(file);

        const div = document.createElement("div");
        div.className = "photo-item";

        div.innerHTML = `
            <img src="${url}">
            <button type="button">✕</button>
        `;

        div.querySelector("button").onclick = () => removeProductPhoto(index);

        container.appendChild(div);
    });
}

function removeProductPhoto(index) {
    productFiles.splice(index, 1);
    renderProductPreview();
}

document.getElementById("d-photo")?.addEventListener("change", e => {
    const newFiles = Array.from(e.target.files);

    dishFiles = [...dishFiles, ...newFiles];

    renderDishPreview();

    e.target.value = "";
});

function renderDishPreview() {
    const container = document.getElementById("d-photo-preview");
    container.innerHTML = "";

    dishFiles.forEach((file, index) => {
        const url = URL.createObjectURL(file);

        const div = document.createElement("div");
        div.className = "photo-item";

        div.innerHTML = `
            <img src="${url}">
            <button type="button">✕</button>
        `;

        div.querySelector("button").onclick = () => removeDishPhoto(index);

        container.appendChild(div);
    });
}
function removeDishPhoto(index) {
    dishFiles.splice(index, 1);
    renderDishPreview();
}

function showTab(tab) {
    document.getElementById("products-section").classList.add("hidden");
    document.getElementById("dishes-section").classList.add("hidden");
    document.getElementById("product-detail-section").classList.add("hidden");

    document.getElementById(tab + "-section").classList.remove("hidden");
}

/* =========================
   📍 PRODUCTS - УТИЛИТЫ
========================= */

function extractProducts(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data.content)) return data.content;

    if (data._embedded) {
        const key = Object.keys(data._embedded)[0];
        return data._embedded[key] || [];
    }

    return [];
}

function getProductFlags() {
    const flags = [];
    document.querySelectorAll(".p-flag:checked").forEach(cb => {
        flags.push(cb.value);
    });
    return flags;
}

/* =========================
   📍 EDIT PRODUCT
========================= */

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

    document.querySelectorAll(".p-flag").forEach(cb => cb.checked = false);

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

/* =========================
   📍 SAVE PRODUCT
========================= */

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
        flags: getProductFlags(),
        photos: []
    };

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(dto)], { type: "application/json" }));

    if (fileInput.files.length > 0) {
        productFiles.forEach(file => {
            formData.append("files", file);
        });
    }

    if (editingProductId) {
        await fetch(`${PRODUCTS_API}/${editingProductId}`, {
            method: "PATCH",
            body: formData
        });

        editingProductId = null;
        existingPhoto = null;
    } else {
        await fetch(PRODUCTS_API, {
            method: "POST",
            body: formData
        });
    }

    document.getElementById("product-form").reset();
    document.getElementById("preview").classList.add("hidden");

    loadProducts();

    productFiles = [];
    renderProductPreview();
}

/* =========================
   📍 LOAD PRODUCTS
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

    const res = await fetch(`${PRODUCTS_API}?${params}`);
    const data = await res.json();

    allProducts = extractProducts(data);
    renderProducts(allProducts);
}

function renderProducts(products) {
    const list = document.getElementById("products-list");
    list.innerHTML = "";

    products.forEach(p => {
        const card = document.createElement("div");
        card.className = "card";

        card.innerHTML = `
            ${p.photos?.[0] ? `<img src="${p.photos[0]}" width="100">` : ""}
            <b>${p.name}</b><br>
            Ккал: ${p.calories}<br>

            <button onclick="openProduct(${p.id})">Открыть</button>
            <button onclick="startEditProduct(${p.id})">Редактировать</button>
            <button onclick="deleteProduct(${p.id})">Удалить</button>
        `;

        list.appendChild(card);
    });
}

async function deleteDish(id) {
    if (!confirm("Удалить блюдо?")) return;

    await fetch(`${DISHES_API}/${id}`, {
        method: "DELETE"
    });

    loadDishes();
}
function calculateDishMacros() {

    if (manualMacros) return; // 👈 не затираем ручные правки

    let calories = 0, proteins = 0, fats = 0, carbs = 0;

    document.querySelectorAll(".ingredient-row").forEach(row => {
        const id = row.querySelector(".ingredient-product").value;
        const amount = +row.querySelector(".ingredient-amount").value;

        const product = allProducts.find(p => p.id == id);
        if (!product) return;

        calories += product.calories * amount / 100;
        proteins += product.proteins * amount / 100;
        fats += product.fats * amount / 100;
        carbs += product.carbohydrates * amount / 100;
    });

    document.getElementById("d-calories").value = calories.toFixed(1);
    document.getElementById("d-proteins").value = proteins.toFixed(1);
    document.getElementById("d-fats").value = fats.toFixed(1);
    document.getElementById("d-carbs").value = carbs.toFixed(1);
}

document.addEventListener("input", e => {
    if (
        e.target.classList.contains("ingredient-amount") ||
        e.target.classList.contains("ingredient-product")
    ) {
        calculateDishMacros();
        updateDishFlagsAvailability();
    }
});

/* =========================
   📍 PREVIEW IMAGE
========================= */

document.addEventListener("DOMContentLoaded", () => {
    const photoInput = document.getElementById("p-photo");

    photoInput?.addEventListener("change", e => {
        const file = e.target.files[0];
        if (!file) return;

        const preview = document.getElementById("preview");
        preview.src = URL.createObjectURL(file);
        preview.classList.remove("hidden");
    });
});

/* =========================
   📍 INGREDIENTS
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
    document.querySelectorAll(".ingredient-product").forEach(select => {
        select.innerHTML = "";

        allProducts.forEach(p => {
            select.innerHTML += `<option value="${p.id}">${p.name}</option>`;
        });
    });
}

/* =========================
   📍 DISH MACROS
========================= */

function calculateDishMacros() {
    let calories = 0, proteins = 0, fats = 0, carbs = 0;

    document.querySelectorAll(".ingredient-row").forEach(row => {
        const id = row.querySelector(".ingredient-product").value;
        const amount = +row.querySelector(".ingredient-amount").value;

        const product = allProducts.find(p => p.id == id);
        if (!product) return;

        calories += product.calories * amount / 100;
        proteins += product.proteins * amount / 100;
        fats += product.fats * amount / 100;
        carbs += product.carbohydrates * amount / 100;
    });

    document.getElementById("d-calories").value = calories.toFixed(1);
    document.getElementById("d-proteins").value = proteins.toFixed(1);
    document.getElementById("d-fats").value = fats.toFixed(1);
    document.getElementById("d-carbs").value = carbs.toFixed(1);
}

/* =========================
   📍 DISH FLAGS LOGIC
========================= */

function updateDishFlagsAvailability() {

    let canVegan = true;
    let canGluten = true;
    let canSugar = true;

    document.querySelectorAll(".ingredient-row").forEach(row => {
        const id = row.querySelector(".ingredient-product").value;
        const product = allProducts.find(p => p.id == id);

        if (!product) return;

        if (!product.flags?.includes("VEGAN")) canVegan = false;
        if (!product.flags?.includes("GLUTEN_FREE")) canGluten = false;
        if (!product.flags?.includes("SUGAR_FREE")) canSugar = false;
    });

    setDishFlagState("VEGAN", canVegan);
    setDishFlagState("GLUTEN_FREE", canGluten);
    setDishFlagState("SUGAR_FREE", canSugar);
}

function setDishFlagState(flag, enabled) {
    const cb = document.querySelector(`.d-flag[value="${flag}"]`);
    if (!cb) return;

    cb.disabled = !enabled;

    if (!enabled) cb.checked = false;
}

function getDishFlagsFromUI() {
    const flags = [];

    document.querySelectorAll(".d-flag").forEach(cb => {
        if (cb.checked && !cb.disabled) {
            flags.push(cb.value);
        }
    });

    return flags;
}

/* =========================
   📍 CREATE DISH
========================= */

async function saveDish() {

    const ingredients = [];

    document.querySelectorAll(".ingredient-row").forEach(row => {
        ingredients.push({
            productId: +row.querySelector(".ingredient-product").value,
            amount: +row.querySelector(".ingredient-amount").value
        });
    });

    const dto = {
        name: document.getElementById("d-name").value,
        portionSize: +document.getElementById("d-portion").value,
        calories: +document.getElementById("d-calories").value,
        proteins: +document.getElementById("d-proteins").value,
        fats: +document.getElementById("d-fats").value,
        carbohydrates: +document.getElementById("d-carbs").value,
        ingredients: ingredients,
        flags: getDishFlagsFromUI()
    };

    if (editDishId) {
        await fetch(`${DISHES_API}/${editDishId}`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(dto)
        });

        editDishId = null;

    } else {
        await fetch(DISHES_API, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(dto)
        });
    }

    clearDishForm();
    loadDishes();

    dishFiles = [];
    renderDishPreview();
}

function clearDishForm() {
    document.getElementById("dish-form").reset();
    document.getElementById("ingredients").innerHTML = "";
}

async function createDish() {
    const fileInput = document.getElementById("d-photo");

    const ingredients = [];

    document.querySelectorAll(".ingredient-row").forEach(row => {
        const productId = +row.querySelector(".ingredient-product").value;
        const amount = +row.querySelector(".ingredient-amount").value;

        if (productId && amount > 0) {
            ingredients.push({ productId, amount });
        }
    });

    const dto = {
        name: document.getElementById("d-name").value,
        calories: +document.getElementById("d-calories").value,
        proteins: +document.getElementById("d-proteins").value,
        fats: +document.getElementById("d-fats").value,
        carbohydrates: +document.getElementById("d-carbs").value,
        ingredients,
        portionSize: +document.getElementById("d-portion").value,
        category: document.getElementById("d-category")?.value || "SECOND",
        flags: getDishFlagsFromUI(),
        photos: []
    };

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(dto)], { type: "application/json" }));

    if (fileInput.files.length) {
        dishFiles.forEach(file => {
            formData.append("files", file);
        });
    }

    await fetch(DISHES_API, {
        method: "POST",
        body: formData
    });

    loadDishes();

    document.getElementById("dish-form").reset();
    document.getElementById("d-preview").classList.add("hidden");
}

/* =========================
   📍 LOAD DISHES + FILTERS
========================= */

async function loadDishes() {

    const params = new URLSearchParams();

    const name = document.getElementById("dish-search")?.value;
    const category = document.getElementById("dish-category")?.value;

    if (name) params.append("name", name);   // 🔥 ВАЖНО
    if (category) params.append("category", category);

    const res = await fetch(`${DISHES_API}?${params.toString()}`);
    const data = await res.json();

    let dishes = data.content || data;

    const vegan = document.getElementById("f-vegan")?.checked;
    const gluten = document.getElementById("f-gluten")?.checked;
    const sugar = document.getElementById("f-sugar")?.checked;

    if (vegan) dishes = dishes.filter(d => d.flags?.includes("VEGAN"));
    if (gluten) dishes = dishes.filter(d => d.flags?.includes("GLUTEN_FREE"));
    if (sugar) dishes = dishes.filter(d => d.flags?.includes("SUGAR_FREE"));

    renderDishes(dishes);
}


function renderDishes(dishes) {
    const list = document.getElementById("dishes-list");
    list.innerHTML = "";

    dishes.forEach(d => {
        const card = document.createElement("div");
        card.className = "card";

        card.innerHTML = `
            ${d.photos?.[0] ? `<img src="${d.photos[0]}" width="100">` : ""}
            <b>${d.name}</b><br>
            Ккал: ${d.calories ?? "-"}<br>
        
            <button onclick="openDish(${d.id})">Открыть</button>
            <button onclick="editDish(${d.id})">Редактировать</button>
            <button onclick="deleteDish(${d.id})">Удалить</button>
`;
        list.appendChild(card);
    });
}

async function editDish(id) {
    const res = await fetch(`${DISHES_API}/${id}`);
    const d = await res.json();

    editDishId = id;

    showTab('dishes');

    document.getElementById("d-name").value = d.name;
    document.getElementById("d-portion").value = d.portionSize;
    document.getElementById("d-calories").value = d.calories;
    document.getElementById("d-proteins").value = d.proteins;
    document.getElementById("d-fats").value = d.fats;
    document.getElementById("d-carbs").value = d.carbohydrates;

    document.getElementById("ingredients").innerHTML = "";

    d.ingredients.forEach(ing => {
        addIngredientRow();

        const rows = document.querySelectorAll(".ingredient-row");
        const row = rows[rows.length - 1];

        row.querySelector(".ingredient-product").value = ing.productId;
        row.querySelector(".ingredient-amount").value = ing.amount;
    });
}
function closeDishView() {
    document.getElementById("dish-detail-section").classList.add("hidden");
    document.getElementById("dishes-section").classList.remove("hidden");
}

async function deleteProduct(id) {
    if (!confirm("Удалить продукт?")) return;

    const res = await fetch(`${PRODUCTS_API}/${id}`, {
        method: "DELETE"
    });

    if (!res.ok) {
        const err = await res.json();
        alert(err.error || "Ошибка удаления");
        return;
    }

    loadProducts();
}


async function openDish(id) {
    const res = await fetch(`${DISHES_API}/${id}`);
    const d = await res.json();

    document.getElementById("dish-detail-section").classList.remove("hidden");
    document.getElementById("dishes-section").classList.add("hidden");

    document.getElementById("dish-detail").innerHTML = `
        <div class="card large">
            ${d.photos?.[0] ? `<img src="${d.photos[0]}" width="200">` : ""}

            <h2>${d.name}</h2>

            <p><b>Ккал:</b> ${d.calories}</p>
            <p><b>Б:</b> ${d.proteins}</p>
            <p><b>Ж:</b> ${d.fats}</p>
            <p><b>У:</b> ${d.carbohydrates}</p>

            <p><b>Порция:</b> ${d.portionSize} г</p>
            <p><b>Категория:</b> ${d.category}</p>
            <p><b>Флаги:</b> ${d.flags?.join(", ") || "-"}</p>

            <h3>Состав</h3>
            ${d.ingredients?.length
        ? d.ingredients.map(i => `
                    <div>
                        ${i.productName} — ${i.amount} г
                    </div>
                `).join("")
        : "<p>Состав не указан</p>"
    }
        </div>
    `;
}

function closeDishView() {
    document.getElementById("dish-detail-section").classList.add("hidden");
    document.getElementById("dishes-section").classList.remove("hidden");
}

/* =========================
   📍 GLOBAL INPUT HANDLER (ОЧИЩЕН)
========================= */

document.addEventListener("input", e => {

    if (
        e.target.classList.contains("ingredient-amount") ||
        e.target.classList.contains("ingredient-product")
    ) {
        calculateDishMacros();
        updateDishFlagsAvailability();
    }
});

/* =========================
   📍 INIT
========================= */

loadProducts();
loadDishes();