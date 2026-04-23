const PRODUCTS_API = "/rest/admin-ui/products";
const DISHES_API = "/rest/admin-ui/dishes";

let allProducts = [];

/* =========================
   ТАБЫ
========================= */

function showTab(tab) {
    document.getElementById("products-section").classList.add("hidden");
    document.getElementById("dishes-section").classList.add("hidden");

    document.getElementById(tab + "-section").classList.remove("hidden");
}

/* =========================
   PRODUCTS
========================= */

async function loadProducts() {
    const response = await fetch(PRODUCTS_API);
    const data = await response.json();

    allProducts = data.content || data;
    renderProducts(allProducts);
}

function renderProducts(products) {
    const list = document.getElementById("products-list");
    list.innerHTML = "";

    products.forEach(p => {
        list.innerHTML += `
            <div class="card">
                ${p.photos?.length ? `<img src="${p.photos[0]}" width="100">` : ""}
                <b>${p.name}</b><br>
                Ккал: ${p.calories}<br>
                Б: ${p.proteins} Ж: ${p.fats} У: ${p.carbohydrates}
            </div>
        `;
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
        list.innerHTML += `
            <div class="card">
                <b>${d.name}</b><br>
                Ккал: ${d.calories}
            </div>
        `;
    });
}

/* =========================
   INIT
========================= */

loadProducts();
loadDishes();