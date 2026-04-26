const PRODUCTS_API = "/rest/admin-ui/products";
const DISHES_API = "/rest/admin-ui/dishes";

/* =========================
   📍 ГЛОБАЛЬНОЕ СОСТОЯНИЕ
========================= */

let allProducts = [];
let editingProductId = null;
let editDishId = null;
let manualMacros = false;

// Продукты: новые файлы + существующие URL фото
// 📌 Бэкенд должен: оставить photos из DTO + добавить файлы из files[]
let productFiles = [];
let productExistingPhotos = [];

// Блюда: новые файлы + существующие URL фото
// 📌 Бэкенд должен: оставить photos из DTO + добавить файлы из files[]
let dishFiles = [];
let dishExistingPhotos = [];

/* =========================
   📍 УТИЛИТЫ ВАЛИДАЦИИ
========================= */

function showAlert(message, type = "error") {
    if (type === "error") {
        alert("❌ Ошибка: " + message);
    } else if (type === "warning") {
        alert("⚠️ Предупреждение: " + message);
    } else {
        alert("✅ " + message);
    }
}

function validateProductName(name) {
    if (!name || name.trim().length < 2) {
        return "Название продукта должно содержать минимум 2 символа";
    }
    return null;
}

function validateProductMacros(calories, proteins, fats, carbs) {
    const totalMacros = proteins + fats + carbs;
    if (totalMacros > 100) {
        return "Сумма БЖУ не может превышать 100 г (сейчас: " + totalMacros.toFixed(1) + " г)";
    }
    if (proteins < 0 || fats < 0 || carbs < 0) {
        return "Значения БЖУ не могут быть отрицательными";
    }
    if (proteins > 100 || fats > 100 || carbs > 100) {
        return "Каждое значение БЖУ не может превышать 100 г";
    }
    if (calories < 0) {
        return "Калорийность не может быть отрицательной";
    }
    return null;
}

function validateProductPhotos(newFiles, existingPhotos) {
    const totalPhotos = existingPhotos.length + newFiles.length;
    if (totalPhotos > 5) {
        return "Максимальное количество фотографий — 5 (сейчас: " + totalPhotos + ")";
    }
    for (const file of newFiles) {
        if (!file.type.startsWith("image/")) {
            return "Файл \"" + file.name + "\" не является изображением";
        }
        if (file.size > 5 * 1024 * 1024) {
            return "Файл \"" + file.name + "\" превышает лимит 5 МБ";
        }
    }
    return null;
}

function validateDishName(name) {
    if (!name || name.trim().length < 2) {
        return "Название блюда должно содержать минимум 2 символа";
    }
    return null;
}

function validatePortionSize(size) {
    if (!size || size <= 0) {
        return "Размер порции должен быть больше 0";
    }
    return null;
}

function validateDishIngredients(ingredients) {
    if (!ingredients || ingredients.length === 0) {
        return "Блюдо должно содержать хотя бы один ингредиент";
    }
    for (let i = 0; i < ingredients.length; i++) {
        const ing = ingredients[i];
        if (!ing.productId) {
            return "В ингредиенте #" + (i + 1) + " не выбран продукт";
        }
        if (!ing.amount || ing.amount <= 0) {
            return "В ингредиенте #" + (i + 1) + " указано некорректное количество";
        }
    }
    return null;
}

function validateDishMacros(calories, proteins, fats, carbs) {
    if (calories < 0 || proteins < 0 || fats < 0 || carbs < 0) {
        return "Значения КБЖУ не могут быть отрицательными";
    }
    return null;
}

/* =========================
   📍 ТАБЫ
========================= */

["d-calories", "d-proteins", "d-fats", "d-carbs"].forEach(id => {
    document.getElementById(id)?.addEventListener("input", () => {
        manualMacros = true;
    });
});

/* =========================
   📍 PRODUCT PHOTO PREVIEW
========================= */

document.getElementById("p-photo")?.addEventListener("change", e => {
    const newFiles = Array.from(e.target.files);

    const currentCount = productExistingPhotos.length + productFiles.length;
    if (currentCount + newFiles.length > 5) {
        showAlert("Можно загрузить максимум 5 фотографий. Сейчас: " + currentCount, "warning");
        e.target.value = "";
        return;
    }

    for (const file of newFiles) {
        if (!file.type.startsWith("image/")) {
            showAlert("Файл \"" + file.name + "\" не является изображением", "error");
            e.target.value = "";
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showAlert("Файл \"" + file.name + "\" превышает лимит 5 МБ", "error");
            e.target.value = "";
            return;
        }
    }

    productFiles = [...productFiles, ...newFiles];
    renderProductPreview();
    e.target.value = "";
});

function renderProductPreview() {
    const container = document.getElementById("p-photo-preview");
    if (!container) return;
    container.innerHTML = "";

    // Рендер существующих фото
    productExistingPhotos.forEach((url, index) => {
        const div = document.createElement("div");
        div.className = "photo-item existing";
        div.dataset.type = "existing";
        div.dataset.index = index;
        div.innerHTML = `
            <img src="${url}" alt="existing photo">
            <button type="button" aria-label="Удалить фото" title="Удалить">✕</button>
        `;
        div.querySelector("button").onclick = () => removeProductExistingPhoto(index);
        container.appendChild(div);
    });

    // Рендер новых файлов
    productFiles.forEach((file, index) => {
        const url = URL.createObjectURL(file);
        const div = document.createElement("div");
        div.className = "photo-item new";
        div.dataset.type = "new";
        div.dataset.index = index;
        div.innerHTML = `
            <img src="${url}" alt="new photo preview">
            <button type="button" aria-label="Удалить фото" title="Удалить">✕</button>
        `;
        div.querySelector("button").onclick = () => removeProductNewPhoto(index);
        container.appendChild(div);
    });
}

function removeProductExistingPhoto(index) {
    productExistingPhotos.splice(index, 1);
    renderProductPreview();
}

function removeProductNewPhoto(index) {
    productFiles.splice(index, 1);
    renderProductPreview();
}

/* =========================
   📍 DISH PHOTO PREVIEW
========================= */

document.getElementById("d-photo")?.addEventListener("change", e => {
    const newFiles = Array.from(e.target.files);

    const currentCount = dishExistingPhotos.length + dishFiles.length;
    if (currentCount + newFiles.length > 5) {
        showAlert("Можно загрузить максимум 5 фотографий. Сейчас: " + currentCount, "warning");
        e.target.value = "";
        return;
    }

    for (const file of newFiles) {
        if (!file.type.startsWith("image/")) {
            showAlert("Файл \"" + file.name + "\" не является изображением", "error");
            e.target.value = "";
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showAlert("Файл \"" + file.name + "\" превышает лимит 5 МБ", "error");
            e.target.value = "";
            return;
        }
    }

    dishFiles = [...dishFiles, ...newFiles];
    renderDishPreview();
    e.target.value = "";
});

function renderDishPreview() {
    const container = document.getElementById("d-photo-preview");
    if (!container) return;
    container.innerHTML = "";

    // Рендер существующих фото
    dishExistingPhotos.forEach((url, index) => {
        const div = document.createElement("div");
        div.className = "photo-item existing";
        div.dataset.type = "existing";
        div.dataset.index = index;
        div.innerHTML = `
            <img src="${url}" alt="existing photo">
            <button type="button" aria-label="Удалить фото" title="Удалить">✕</button>
        `;
        div.querySelector("button").onclick = () => removeDishExistingPhoto(index);
        container.appendChild(div);
    });

    // Рендер новых файлов
    dishFiles.forEach((file, index) => {
        const url = URL.createObjectURL(file);
        const div = document.createElement("div");
        div.className = "photo-item new";
        div.dataset.type = "new";
        div.dataset.index = index;
        div.innerHTML = `
            <img src="${url}" alt="new photo preview">
            <button type="button" aria-label="Удалить фото" title="Удалить">✕</button>
        `;
        div.querySelector("button").onclick = () => removeDishNewPhoto(index);
        container.appendChild(div);
    });
}

function removeDishExistingPhoto(index) {
    dishExistingPhotos.splice(index, 1);
    renderDishPreview();
}

function removeDishNewPhoto(index) {
    dishFiles.splice(index, 1);
    renderDishPreview();
}

function showTab(tab) {
    document.getElementById("products-section")?.classList.add("hidden");
    document.getElementById("dishes-section")?.classList.add("hidden");
    document.getElementById("product-detail-section")?.classList.add("hidden");
    document.getElementById("dish-detail-section")?.classList.add("hidden");

    const section = document.getElementById(tab + "-section");
    if (section) {
        section.classList.remove("hidden");
    }
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
    try {
        const res = await fetch(`${PRODUCTS_API}/${id}`);
        if (!res.ok) {
            showAlert("Не удалось загрузить продукт", "error");
            return;
        }
        const p = await res.json();

        editingProductId = id;

        // Сброс и заполнение фото
        productFiles = [];
        productExistingPhotos = p.photos || [];

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

        renderProductPreview();
        showTab('products');
    } catch (err) {
        console.error(err);
        showAlert("Ошибка при загрузке продукта", "error");
    }
}

/* =========================
   📍 SAVE PRODUCT
========================= */

async function saveProduct() {
    const name = document.getElementById("p-name").value.trim();
    const nameError = validateProductName(name);
    if (nameError) {
        showAlert(nameError, "error");
        return;
    }

    const calories = +document.getElementById("p-calories").value;
    const proteins = +document.getElementById("p-proteins").value;
    const fats = +document.getElementById("p-fats").value;
    const carbs = +document.getElementById("p-carbs").value;

    const macrosError = validateProductMacros(calories, proteins, fats, carbs);
    if (macrosError) {
        showAlert(macrosError, "error");
        return;
    }

    const category = document.getElementById("p-category").value;
    if (!category) {
        showAlert("Выберите категорию продукта", "error");
        return;
    }

    const cooking = document.getElementById("p-cooking").value;
    if (!cooking) {
        showAlert("Укажите необходимость готовки", "error");
        return;
    }

    const photoError = validateProductPhotos(productFiles, productExistingPhotos);
    if (photoError) {
        showAlert(photoError, "error");
        return;
    }

    // 📌 Бэкенд должен: оставить эти photos + добавить файлы из files[]
    const dto = {
        name: name,
        calories,
        proteins,
        fats,
        carbohydrates: carbs,
        category: category,
        cookingRequirement: cooking,
        flags: getProductFlags(),
        photos: [...productExistingPhotos]
    };

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(dto)], { type: "application/json" }));

    if (productFiles.length > 0) {
        productFiles.forEach(file => {
            formData.append("files", file);
        });
    }

    try {
        if (editingProductId) {
            const res = await fetch(`${PRODUCTS_API}/${editingProductId}`, {
                method: "PATCH",
                body: formData
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                showAlert(err.message || "Ошибка при обновлении продукта", "error");
                return;
            }
            editingProductId = null;
            showAlert("Продукт успешно обновлен", "success");
        } else {
            const res = await fetch(PRODUCTS_API, {
                method: "POST",
                body: formData
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                showAlert(err.message || "Ошибка при создании продукта", "error");
                return;
            }
            showAlert("Продукт успешно создан", "success");
        }

        document.getElementById("product-form")?.reset();
        document.getElementById("p-photo-preview").innerHTML = "";
        loadProducts();
        productFiles = [];
        productExistingPhotos = [];
    } catch (err) {
        console.error(err);
        showAlert("Сетевая ошибка при сохранении продукта", "error");
    }
}

function cancelEditDish() {
    editDishId = null;
    document.getElementById("dish-form")?.reset();
    document.getElementById("ingredients").innerHTML = "";
    document.getElementById("d-photo-preview").innerHTML = "";
    dishFiles = [];
    dishExistingPhotos = [];
    document.getElementById("d-calories").value = "0";
    document.getElementById("d-proteins").value = "0";
    document.getElementById("d-fats").value = "0";
    document.getElementById("d-carbs").value = "0";
    manualMacros = false;
    document.querySelectorAll(".d-flag").forEach(cb => {
        cb.checked = false;
        cb.disabled = false;
    });
}

function extractCategoryFromName(name) {
    const map = {
        "!десерт": "DESSERT",
        "!первое": "FIRST",
        "!второе": "SECOND",
        "!напиток": "DRINK",
        "!салат": "SALAD",
        "!суп": "SOUP",
        "!перекус": "SNACK"
    };
    const parts = name.split(" ");
    for (const part of parts) {
        if (map[part]) {
            return {
                category: map[part],
                cleanName: name.replace(part, "").trim()
            };
        }
    }
    return { category: null, cleanName: name };
}

function cancelEditProduct() {
    editingProductId = null;
    productFiles = [];
    productExistingPhotos = [];
    document.getElementById("product-form")?.reset();
    document.getElementById("p-photo-preview").innerHTML = "";
    document.querySelectorAll(".p-flag").forEach(cb => cb.checked = false);
}

/* =========================
   📍 LOAD PRODUCTS
========================= */

async function loadProducts() {
    try {
        const params = new URLSearchParams();
        const search = document.getElementById("product-search")?.value;
        const category = document.getElementById("filter-category")?.value;
        const cooking = document.getElementById("filter-cooking")?.value;
        const sort = document.getElementById("sort")?.value;

        if (search) params.append("search", search);
        if (category) params.append("category", category);
        if (cooking) params.append("cookingRequirement", cooking);
        if (sort) {
            const [field, dir] = sort.split(",");
            params.append("sort", `${field},${dir}`);
        }

        const res = await fetch(`${PRODUCTS_API}?${params}`);
        if (!res.ok) {
            showAlert("Не удалось загрузить список продуктов", "error");
            return;
        }
        const data = await res.json();
        allProducts = extractProducts(data);

        const vegan = document.getElementById("p-f-vegan")?.checked;
        const gluten = document.getElementById("p-f-gluten")?.checked;
        const sugar = document.getElementById("p-f-sugar")?.checked;

        let products = allProducts;
        if (vegan) products = products.filter(p => p.flags?.includes("VEGAN"));
        if (gluten) products = products.filter(p => p.flags?.includes("GLUTEN_FREE"));
        if (sugar) products = products.filter(p => p.flags?.includes("SUGAR_FREE"));

        renderProducts(products);
    } catch (err) {
        console.error(err);
        showAlert("Ошибка сети при загрузке продуктов", "error");
    }
}

function renderProducts(products) {
    const list = document.getElementById("products-list");
    if (!list) return;
    list.innerHTML = "";

    if (products.length === 0) {
        list.innerHTML = "<p>Продукты не найдены</p>";
        return;
    }

    products.forEach(p => {
        const card = document.createElement("div");
        card.className = "card";
        const firstPhoto = p.photos?.[0];
        card.innerHTML = `
            ${firstPhoto ? `<img src="${firstPhoto}" width="100" alt="${p.name}" loading="lazy">` : ""}
            <b>${p.name}</b><br>
            Ккал: ${p.calories}<br>
            <button onclick="openProduct(${p.id})">Открыть</button>
            <button onclick="startEditProduct(${p.id})">Редактировать</button>
            <button onclick="deleteProduct(${p.id})">Удалить</button>
        `;
        list.appendChild(card);
    });
}

function renderGallery(photos = []) {
    if (!photos || photos.length === 0) return "<p>Фото нет</p>";
    return `
        <div class="gallery">
            ${photos.map(p => `<img src="${p}" class="gallery-img" alt="photo" loading="lazy">`).join("")}
        </div>
    `;
}

/* =========================
   📍 DELETE PRODUCT
========================= */

async function deleteProduct(id) {
    try {
        const dishesRes = await fetch(`${DISHES_API}?productId=${id}`);
        if (dishesRes.ok) {
            const dishesData = await dishesRes.json();
            const dishes = dishesData.content || dishesData || [];
            const usingDishes = dishes.filter(d =>
                d.ingredients?.some(ing => ing.productId === id)
            );

            if (usingDishes.length > 0) {
                const dishNames = usingDishes.map(d => d.name).join(", ");
                showAlert(
                    "Нельзя удалить продукт, который используется в блюдах: " + dishNames,
                    "warning"
                );
                return;
            }
        }

        if (!confirm("Удалить продукт? Это действие нельзя отменить.")) return;

        const res = await fetch(`${PRODUCTS_API}/${id}`, { method: "DELETE" });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            showAlert(err.message || "Ошибка удаления продукта", "error");
            return;
        }

        showAlert("Продукт удалён", "success");
        loadProducts();
    } catch (err) {
        console.error(err);
        showAlert("Сетевая ошибка при удалении продукта", "error");
    }
}

/* =========================
   📍 DISH MACROS CALCULATION
========================= */

function calculateDishMacros() {
    if (manualMacros) return;

    let calories = 0;
    let proteins = 0;
    let fats = 0;
    let carbs = 0;

    document.querySelectorAll(".ingredient-row").forEach(row => {
        const id = row.querySelector(".ingredient-product")?.value;
        const amount = +row.querySelector(".ingredient-amount")?.value;
        const product = allProducts.find(p => p.id == id);

        if (!product || !amount || amount <= 0) return;

        calories += product.calories * amount / 100;
        proteins += product.proteins * amount / 100;
        fats += product.fats * amount / 100;
        carbs += product.carbohydrates * amount / 100;
    });

    const calEl = document.getElementById("d-calories");
    const proEl = document.getElementById("d-proteins");
    const fatEl = document.getElementById("d-fats");
    const carbEl = document.getElementById("d-carbs");

    if (calEl) calEl.value = calories.toFixed(1);
    if (proEl) proEl.value = proteins.toFixed(1);
    if (fatEl) fatEl.value = fats.toFixed(1);
    if (carbEl) carbEl.value = carbs.toFixed(1);
}

document.addEventListener("input", e => {
    if (
        e.target.classList?.contains("ingredient-amount") ||
        e.target.classList?.contains("ingredient-product")
    ) {
        calculateDishMacros();
        updateDishFlagsAvailability();
    }
});

/* =========================
   📍 PREVIEW IMAGE (PRODUCT - SINGLE)
========================= */

document.addEventListener("DOMContentLoaded", () => {
    const photoInput = document.getElementById("p-photo");
    photoInput?.addEventListener("change", e => {
        const file = e.target.files?.[0];
        if (!file) return;

        if (!file.type.startsWith("image/")) {
            showAlert("Выбранный файл не является изображением", "error");
            e.target.value = "";
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showAlert("Файл превышает лимит 5 МБ", "error");
            e.target.value = "";
            return;
        }

        const preview = document.getElementById("preview");
        if (preview) {
            preview.src = URL.createObjectURL(file);
            preview.classList.remove("hidden");
        }
    });
});

/* =========================
   📍 INGREDIENTS
========================= */

function addIngredientRow() {
    const container = document.getElementById("ingredients");
    if (!container) return;

    const row = document.createElement("div");
    row.className = "ingredient-row";

    const select = document.createElement("select");
    select.className = "ingredient-product";
    select.innerHTML = '<option value="">-- Выберите продукт --</option>';

    if (allProducts.length === 0) {
        const opt = document.createElement("option");
        opt.textContent = "Сначала добавьте продукты";
        opt.disabled = true;
        select.appendChild(opt);
    } else {
        allProducts.forEach(p => {
            const option = document.createElement("option");
            option.value = p.id;
            option.textContent = p.name;
            select.appendChild(option);
        });
    }

    const input = document.createElement("input");
    input.className = "ingredient-amount";
    input.type = "number";
    input.placeholder = "0";
    input.min = "0";
    input.step = "0.1";

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.textContent = "✕";
    removeBtn.setAttribute("aria-label", "Удалить ингредиент");
    removeBtn.onclick = () => {
        row.remove();
        calculateDishMacros();
        updateDishFlagsAvailability();
    };

    row.appendChild(select);
    row.appendChild(input);
    row.appendChild(removeBtn);
    container.appendChild(row);
}

/* =========================
   📍 DISH MACROS
========================= */

function clearDishForm() {
    document.getElementById("dish-form")?.reset();
    document.getElementById("ingredients").innerHTML = "";
    manualMacros = false;
}

/* =========================
   📍 DISH FLAGS LOGIC
========================= */

function updateDishFlagsAvailability() {
    const rows = document.querySelectorAll(".ingredient-row");
    let canVegan = true;
    let canGluten = true;
    let canSugar = true;

    if (!rows.length) {
        document.querySelectorAll(".d-flag").forEach(cb => {
            cb.disabled = true;
            cb.checked = false;
        });
        return;
    }

    rows.forEach(row => {
        const id = row.querySelector(".ingredient-product")?.value;
        const product = allProducts.find(p => p.id == id);

        if (!product) {
            canVegan = false;
            canGluten = false;
            canSugar = false;
            return;
        }

        const flags = product.flags || [];
        canVegan = canVegan && flags.includes("VEGAN");
        canGluten = canGluten && flags.includes("GLUTEN_FREE");
        canSugar = canSugar && flags.includes("SUGAR_FREE");
    });

    setDishFlagState("VEGAN", canVegan);
    setDishFlagState("GLUTEN_FREE", canGluten);
    setDishFlagState("SUGAR_FREE", canSugar);
}

document.addEventListener("input", handleDishChange);
document.addEventListener("change", handleDishChange);
document.addEventListener("click", handleDishChange);

function handleDishChange(e) {
    if (
        e.target.classList?.contains("ingredient-amount") ||
        e.target.classList?.contains("ingredient-product")
    ) {
        manualMacros = false;
        calculateDishMacros();
        updateDishFlagsAvailability();
    }
}

function setDishFlagState(flag, enabled) {
    const cb = document.querySelector(`.d-flag[value="${flag}"]`);
    if (!cb) return;
    cb.disabled = !enabled;
    cb.checked = enabled;
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
   📍 SAVE DISH
========================= */

async function saveDish() {
    const rawName = document.getElementById("d-name")?.value.trim();
    const nameError = validateDishName(rawName);
    if (nameError) {
        showAlert(nameError, "error");
        return;
    }

    const { category: categoryFromName, cleanName } = extractCategoryFromName(rawName);
    const selectedCategory = document.getElementById("d-category")?.value;
    const finalCategory = selectedCategory || categoryFromName;

    if (!finalCategory) {
        showAlert("Выберите категорию блюда", "error");
        return;
    }

    const portionSize = +document.getElementById("d-portion")?.value;
    const portionError = validatePortionSize(portionSize);
    if (portionError) {
        showAlert(portionError, "error");
        return;
    }

    const ingredients = [];
    document.querySelectorAll(".ingredient-row").forEach(row => {
        ingredients.push({
            productId: +row.querySelector(".ingredient-product").value,
            amount: +row.querySelector(".ingredient-amount").value
        });
    });

    const ingredientsError = validateDishIngredients(ingredients);
    if (ingredientsError) {
        showAlert(ingredientsError, "error");
        return;
    }

    const calories = +document.getElementById("d-calories")?.value;
    const proteins = +document.getElementById("d-proteins")?.value;
    const fats = +document.getElementById("d-fats")?.value;
    const carbs = +document.getElementById("d-carbs")?.value;

    const macrosError = validateDishMacros(calories, proteins, fats, carbs);
    if (macrosError) {
        showAlert(macrosError, "error");
        return;
    }

    if (dishExistingPhotos.length + dishFiles.length > 5) {
        showAlert("Максимальное количество фотографий блюда — 5", "error");
        return;
    }
    for (const file of dishFiles) {
        if (!file.type.startsWith("image/")) {
            showAlert("Файл \"" + file.name + "\" не является изображением", "error");
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showAlert("Файл \"" + file.name + "\" превышает лимит 5 МБ", "error");
            return;
        }
    }

    // 📌 Бэкенд должен: оставить эти photos + добавить файлы из files[]
    const dto = {
        name: cleanName,
        portionSize: portionSize,
        calories: calories,
        proteins: proteins,
        fats: fats,
        carbohydrates: carbs,
        ingredients: ingredients,
        category: finalCategory,
        flags: getDishFlagsFromUI(),
        photos: [...dishExistingPhotos]
    };

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(dto)], { type: "application/json" }));

    if (dishFiles.length > 0) {
        dishFiles.forEach(file => {
            formData.append("files", file);
        });
    }

    try {
        const url = editDishId ? `${DISHES_API}/${editDishId}` : DISHES_API;
        const method = editDishId ? "PATCH" : "POST";

        const res = await fetch(url, { method, body: formData });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            showAlert(err.message || "Ошибка при сохранении блюда", "error");
            return;
        }

        editDishId = null;
        clearDishForm();
        loadDishes();
        dishFiles = [];
        dishExistingPhotos = [];
        renderDishPreview();
        showAlert("Блюдо успешно сохранено", "success");
    } catch (err) {
        console.error(err);
        showAlert("Сетевая ошибка при сохранении блюда", "error");
    }
}

/* =========================
   📍 LOAD DISHES + FILTERS
========================= */

async function loadDishes() {
    try {
        const params = new URLSearchParams();
        const name = document.getElementById("dish-search")?.value;
        const category = document.getElementById("dish-category")?.value;

        if (name) params.append("name", name);
        if (category) params.append("category", category);

        const res = await fetch(`${DISHES_API}?${params.toString()}`);
        if (!res.ok) {
            showAlert("Не удалось загрузить список блюд", "error");
            return;
        }
        const data = await res.json();
        let dishes = data.content || data || [];

        const vegan = document.getElementById("f-vegan")?.checked;
        const gluten = document.getElementById("f-gluten")?.checked;
        const sugar = document.getElementById("f-sugar")?.checked;

        if (vegan) dishes = dishes.filter(d => d.flags?.includes("VEGAN"));
        if (gluten) dishes = dishes.filter(d => d.flags?.includes("GLUTEN_FREE"));
        if (sugar) dishes = dishes.filter(d => d.flags?.includes("SUGAR_FREE"));

        renderDishes(dishes);
    } catch (err) {
        console.error(err);
        showAlert("Ошибка сети при загрузке блюд", "error");
    }
}

function renderDishes(dishes) {
    const list = document.getElementById("dishes-list");
    if (!list) return;
    list.innerHTML = "";

    if (dishes.length === 0) {
        list.innerHTML = "<p>Блюда не найдены</p>";
        return;
    }

    dishes.forEach(d => {
        const card = document.createElement("div");
        card.className = "card";
        const firstPhoto = d.photos?.[0];
        card.innerHTML = `
            ${firstPhoto ? `<img src="${firstPhoto}" width="100" alt="${d.name}" loading="lazy">` : ""}
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
    try {
        const res = await fetch(`${DISHES_API}/${id}`);
        if (!res.ok) {
            showAlert("Не удалось загрузить блюдо", "error");
            return;
        }
        const d = await res.json();
        editDishId = id;
        showTab('dishes');

        // Сброс и заполнение фото
        dishFiles = [];
        dishExistingPhotos = d.photos || [];

        document.getElementById("d-name").value = d.name || "";
        document.getElementById("d-portion").value = d.portionSize || "";
        document.getElementById("d-calories").value = d.calories ?? "0";
        document.getElementById("d-proteins").value = d.proteins ?? "0";
        document.getElementById("d-fats").value = d.fats ?? "0";
        document.getElementById("d-carbs").value = d.carbohydrates ?? "0";
        document.getElementById("d-category").value = d.category || "";

        document.getElementById("ingredients").innerHTML = "";
        d.ingredients?.forEach(ing => {
            addIngredientRow();
            const rows = document.querySelectorAll(".ingredient-row");
            const row = rows[rows.length - 1];
            if (row) {
                row.querySelector(".ingredient-product").value = ing.productId;
                row.querySelector(".ingredient-amount").value = ing.amount;
            }
        });

        renderDishPreview();

        setTimeout(() => {
            updateDishFlagsAvailability();
        }, 0);
    } catch (err) {
        console.error(err);
        showAlert("Ошибка при загрузке блюда", "error");
    }
}

function closeDishView() {
    document.getElementById("dish-detail-section")?.classList.add("hidden");
    document.getElementById("dishes-section")?.classList.remove("hidden");
}

async function deleteDish(id) {
    if (!confirm("Удалить блюдо? Это действие нельзя отменить.")) return;

    try {
        const res = await fetch(`${DISHES_API}/${id}`, { method: "DELETE" });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            showAlert(err.message || "Ошибка удаления блюда", "error");
            return;
        }
        showAlert("Блюдо удалено", "success");
        loadDishes();
    } catch (err) {
        console.error(err);
        showAlert("Сетевая ошибка при удалении блюда", "error");
    }
}

async function openDish(id) {
    try {
        const res = await fetch(`${DISHES_API}/${id}`);
        if (!res.ok) {
            showAlert("Не удалось загрузить блюдо", "error");
            return;
        }
        const d = await res.json();

        document.getElementById("dish-detail-section")?.classList.remove("hidden");
        document.getElementById("dishes-section")?.classList.add("hidden");

        document.getElementById("dish-detail").innerHTML = `
            <div class="card large">
                ${renderGallery(d.photos)}
                <h2>${d.name}</h2>
                <p><b>Ккал:</b> ${d.calories ?? "-"}</p>
                <p><b>Б:</b> ${d.proteins ?? "-"}</p>
                <p><b>Ж:</b> ${d.fats ?? "-"}</p>
                <p><b>У:</b> ${d.carbohydrates ?? "-"}</p>
                <p><b>Порция:</b> ${d.portionSize ?? "-"} г</p>
                <p><b>Категория:</b> ${d.category || "-"}</p>
                <p><b>Флаги:</b> ${d.flags?.join(", ") || "-"}</p>
                <h3>Состав</h3>
                ${d.ingredients?.length
            ? d.ingredients.map(i => `<div>${i.productName || "Продукт #" + i.productId} — ${i.amount} г</div>`).join("")
            : "<p>Состав не указан</p>"
        }
            </div>
        `;
    } catch (err) {
        console.error(err);
        showAlert("Ошибка при отображении блюда", "error");
    }
}

function closeProductView() {
    document.getElementById("product-detail-section")?.classList.add("hidden");
    document.getElementById("products-section")?.classList.remove("hidden");
}

async function openProduct(id) {
    try {
        const res = await fetch(`${PRODUCTS_API}/${id}`);
        if (!res.ok) {
            showAlert("Не удалось загрузить продукт", "error");
            return;
        }
        const p = await res.json();

        document.getElementById("product-detail-section")?.classList.remove("hidden");
        document.getElementById("products-section")?.classList.add("hidden");

        document.getElementById("product-detail").innerHTML = `
            <div class="card large">
                ${renderGallery(p.photos)}
                <h2>${p.name}</h2>
                <p><b>Ккал:</b> ${p.calories}</p>
                <p><b>Белки:</b> ${p.proteins}</p>
                <p><b>Жиры:</b> ${p.fats}</p>
                <p><b>Углеводы:</b> ${p.carbohydrates}</p>
                <p><b>Категория:</b> ${p.category}</p>
                <p><b>Готовка:</b> ${p.cookingRequirement}</p>
                <p><b>Флаги:</b> ${p.flags?.join(", ") || "-"}</p>
            </div>
        `;
    } catch (err) {
        console.error(err);
        showAlert("Ошибка при отображении продукта", "error");
    }
}

/* =========================
   📍 GLOBAL INPUT HANDLER
========================= */

document.addEventListener("input", (e) => {
    if (
        e.target.classList?.contains("ingredient-amount") ||
        e.target.classList?.contains("ingredient-product")
    ) {
        calculateDishMacros();
        updateDishFlagsAvailability();
    }
});

/* =========================
   📍 INIT
========================= */

document.addEventListener("DOMContentLoaded", () => {
    // Устанавливаем дефолтные значения 0 для числовых полей
    const numberInputs = [
        "p-calories", "p-proteins", "p-fats", "p-carbs",
        "d-calories", "d-proteins", "d-fats", "d-carbs", "d-portion"
    ];
    numberInputs.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            if (el.tagName === "INPUT") {
                el.placeholder = "0";
                if (!el.value) el.value = "0";
            }
        }
    });

    loadProducts();
    loadDishes();
});