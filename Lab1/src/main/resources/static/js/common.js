'use strict';

const CATEGORIES = ['SCOUT', 'AGGRESSOR', 'TACTICAL', 'LIBRARIAN', 'APOTHECARY'];
const WEAPONS = ['HEAVY_BOLTGUN', 'MELTAGUN', 'COMBI_PLASMA_GUN', 'FLAMER', 'HEAVY_FLAMER'];

class ApiError extends Error {
  constructor(status, data) {
    super((data && data.message) || ('Ошибка ' + status));
    this.status = status;
    this.data = data;
  }
  /** Список понятных сообщений для показа пользователю. */
  get messages() {
    if (this.data && Array.isArray(this.data.errors) && this.data.errors.length) {
      return this.data.errors.map(e => e.message);
    }
    return [this.message];
  }
}

async function api(method, url, body) {
  const res = await fetch(url, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) {
    location.href = '/login.html';
    throw new ApiError(401, { message: 'Требуется вход в систему' });
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = { message: text }; }
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtDate(s) {
  return s ? new Date(s).toLocaleString('ru-RU') : '';
}

function showErrors(ul, messages) {
  ul.innerHTML = messages.map(m => '<li>' + esc(m) + '</li>').join('');
}

/* ---------- меню ---------- */

function renderMenu(active) {
  const items = [['index.html', 'Объекты'], ['chapters.html', 'Ордена'], ['special.html', 'Спецоперации']];
  document.getElementById('menu').innerHTML =
    items.map(([href, title]) => `<a href="/${href}" class="${href === active ? 'active' : ''}">${title}</a>`).join('') +
    '<span class="spacer"></span><span id="whoami"></span><a href="#" id="logout">Выход</a>';
  api('GET', '/api/auth/me').then(u => { document.getElementById('whoami').textContent = u.username; }).catch(() => {});
  document.getElementById('logout').onclick = async (e) => {
    e.preventDefault();
    await fetch('/logout', { method: 'POST' });
    location.href = '/login.html';
  };
}

/* ---------- изменения в реальном времени (SSE) ---------- */

function subscribeChanges(handler) {
  let timer;
  const es = new EventSource('/api/events');
  es.addEventListener('change', ev => {
    clearTimeout(timer);
    timer = setTimeout(() => handler(JSON.parse(ev.data)), 150);
  });
  return es;
}

/* ---------- карточки объектов ---------- */

function chapterCardHtml(c) {
  return `<dl>
    <dt>ID</dt><dd>${c.id}</dd>
    <dt>Название</dt><dd>${esc(c.name)}</dd>
    <dt>Легион</dt><dd>${esc(c.parentLegion || '—')}</dd>
    <dt>Численность</dt><dd>${c.marinesCount}</dd>
    <dt>Мир</dt><dd>${esc(c.world)}</dd></dl>`;
}

function marineCardHtml(m) {
  return `<h3>Десантник #${m.id}</h3><dl>
    <dt>Имя</dt><dd>${esc(m.name)}</dd>
    <dt>Координаты</dt><dd>(${m.x}; ${m.y})</dd>
    <dt>Создан</dt><dd>${fmtDate(m.creationDate)}</dd>
    <dt>Здоровье</dt><dd>${m.health}</dd>
    <dt>Рост</dt><dd>${m.height}</dd>
    <dt>Категория</dt><dd>${m.category}</dd>
    <dt>Оружие</dt><dd>${m.weaponType}</dd></dl>
    <h4>Связанный объект: орден</h4>${chapterCardHtml(m.chapter)}`;
}

/* ---------- форма десантника (используется на нескольких страницах) ---------- */

function marineFieldsHtml() {
  const opts = list => list.map(v => `<option value="${v}">${v}</option>`).join('');
  return `
    <label>Имя<input name="name" maxlength="255" required></label>
    <label>Координата X (больше -634)<input name="x" type="number" step="any" required></label>
    <label>Координата Y (больше -126)<input name="y" type="number" step="any" required></label>
    <label>Орден<select name="chapterId" required></select></label>
    <label>Здоровье (больше 0)<input name="health" type="number" step="1" min="1" required></label>
    <label>Рост<input name="height" type="number" step="1" required></label>
    <label>Категория<select name="category" required>${opts(CATEGORIES)}</select></label>
    <label>Оружие<select name="weaponType" required>${opts(WEAPONS)}</select></label>`;
}

function num(v) {
  return v === '' || v == null ? null : Number(v);
}

function readMarine(form) {
  const e = form.elements; // form.name — это имя самой формы, поэтому обращаемся через elements
  return {
    name: e.name.value,
    x: num(e.x.value),
    y: num(e.y.value),
    chapterId: num(e.chapterId.value),
    health: num(e.health.value),
    height: num(e.height.value),
    category: e.category.value || null,
    weaponType: e.weaponType.value || null,
  };
}

function fillMarine(form, m) {
  const e = form.elements;
  e.name.value = m ? m.name : '';
  e.x.value = m ? m.x : '';
  e.y.value = m ? m.y : '';
  e.health.value = m ? m.health : '';
  e.height.value = m ? m.height : '';
  e.category.value = m ? m.category : CATEGORIES[0];
  e.weaponType.value = m ? m.weaponType : WEAPONS[0];
  if (m) e.chapterId.value = String(m.chapter.id);
}

/** Проверка на клиенте: возвращает список сообщений об ошибках. */
function validateMarine(m) {
  const errs = [];
  if (!m.name || !m.name.trim()) errs.push('Имя не может быть пустым');
  if (m.x == null || Number.isNaN(m.x)) errs.push('Координата X обязательна и должна быть числом');
  else if (!(m.x > -634)) errs.push('Координата X должна быть больше -634');
  if (m.y == null || Number.isNaN(m.y)) errs.push('Координата Y обязательна и должна быть числом');
  else if (!(m.y > -126)) errs.push('Координата Y должна быть больше -126');
  if (m.chapterId == null) errs.push('Выберите орден (если их нет — создайте на странице «Ордена»)');
  if (m.health == null || !Number.isInteger(m.health) || m.health <= 0) errs.push('Здоровье должно быть целым числом больше 0');
  if (m.height == null || !Number.isInteger(m.height) || Math.abs(m.height) > 2147483647) errs.push('Рост должен быть целым числом');
  if (!m.category) errs.push('Выберите категорию');
  if (!m.weaponType) errs.push('Выберите оружие');
  return errs;
}

/** Заполняет select орденами. exclude — id ордена, который нужно исключить. Возвращает список орденов. */
async function fillChapterSelect(select, selected, exclude) {
  const list = (await api('GET', '/api/chapters')).filter(c => c.id !== exclude);
  select.innerHTML = list.map(c => `<option value="${c.id}">${c.id} — ${esc(c.name)} (${esc(c.world)})</option>`).join('');
  if (selected != null && list.some(c => c.id === selected)) select.value = String(selected);
  return list;
}
