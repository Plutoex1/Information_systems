'use strict';

const COLUMNS = [
  { key: 'id', label: 'ID' },
  { key: 'name', label: 'Имя', filter: true },
  { key: 'x', label: 'X' },
  { key: 'y', label: 'Y' },
  { key: 'creationDate', label: 'Создан' },
  { key: 'chapterName', label: 'Орден', filter: true },
  { key: 'parentLegion', label: 'Легион', filter: true },
  { key: 'marinesCount', label: 'Числ. ордена' },
  { key: 'world', label: 'Мир', filter: true },
  { key: 'health', label: 'Здоровье' },
  { key: 'height', label: 'Рост' },
  { key: 'category', label: 'Категория', filter: true },
  { key: 'weaponType', label: 'Оружие', filter: true },
];

const state = { page: 0, size: 10, sort: 'id', dir: 'asc', filters: {} };
let editingId = null;
let infoId = null;

const $ = id => document.getElementById(id);

function cellValue(m, key) {
  switch (key) {
    case 'chapterName': return m.chapter.name;
    case 'parentLegion': return m.chapter.parentLegion ?? '';
    case 'marinesCount': return m.chapter.marinesCount;
    case 'world': return m.chapter.world;
    case 'creationDate': return fmtDate(m.creationDate);
    default: return m[key];
  }
}

/* ---------- таблица ---------- */

function renderHeader() {
  const sorts = COLUMNS.map(c => `<th data-sort="${c.key}">${c.label}<span class="arrow"></span></th>`).join('');
  const filters = COLUMNS.map(c => c.filter
    ? `<th><input data-filter="${c.key}" placeholder="фильтр"></th>` : '<th></th>').join('');
  $('thead').innerHTML = `<tr class="sorts">${sorts}<th>Действия</th></tr><tr class="filters">${filters}<th></th></tr>`;

  $('thead').querySelectorAll('th[data-sort]').forEach(th => {
    th.onclick = () => {
      const key = th.dataset.sort;
      if (state.sort === key) state.dir = state.dir === 'asc' ? 'desc' : 'asc';
      else { state.sort = key; state.dir = 'asc'; }
      state.page = 0;
      updateArrows();
      load();
    };
  });

  let timer;
  $('thead').querySelectorAll('input[data-filter]').forEach(input => {
    input.oninput = () => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        state.filters[input.dataset.filter] = input.value.trim();
        state.page = 0;
        load();
      }, 300);
    };
  });
  updateArrows();
}

function updateArrows() {
  $('thead').querySelectorAll('th[data-sort]').forEach(th => {
    th.querySelector('.arrow').textContent =
      th.dataset.sort === state.sort ? (state.dir === 'asc' ? ' ▲' : ' ▼') : '';
  });
}

async function load() {
  const params = new URLSearchParams({ page: state.page, size: state.size, sort: state.sort, dir: state.dir });
  for (const [k, v] of Object.entries(state.filters)) if (v) params.set(k, v);
  try {
    const data = await api('GET', '/api/marines?' + params);
    if (data.totalPages > 0 && data.page >= data.totalPages) {
      state.page = data.totalPages - 1;
      return load();
    }
    renderRows(data);
  } catch (err) {
    $('tbody').innerHTML = `<tr><td colspan="${COLUMNS.length + 1}" class="muted">${esc(err.message)}</td></tr>`;
  }
}

function renderRows(data) {
  if (data.items.length === 0) {
    $('tbody').innerHTML = `<tr><td colspan="${COLUMNS.length + 1}" class="muted">Нет объектов</td></tr>`;
  } else {
    $('tbody').innerHTML = data.items.map(m => `<tr>
      ${COLUMNS.map(c => `<td>${esc(cellValue(m, c.key))}</td>`).join('')}
      <td class="actions">
        <button data-act="info" data-id="${m.id}">Инфо</button>
        <button data-act="edit" data-id="${m.id}">Изменить</button>
        <button data-act="delete" data-id="${m.id}" class="danger">Удалить</button>
      </td></tr>`).join('');
  }
  $('pageInfo').textContent = `Страница ${data.totalPages === 0 ? 0 : data.page + 1} из ${data.totalPages}`;
  $('total').textContent = `Всего: ${data.total}`;
  $('prev').disabled = data.page <= 0;
  $('next').disabled = data.page + 1 >= data.totalPages;
}

$('tbody').onclick = async (ev) => {
  const btn = ev.target.closest('button[data-act]');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.act === 'info') openInfo(id);
  if (btn.dataset.act === 'edit') openEdit(id);
  if (btn.dataset.act === 'delete') removeMarine(id);
};

$('prev').onclick = () => { state.page--; load(); };
$('next').onclick = () => { state.page++; load(); };
$('pageSize').onchange = (e) => { state.size = Number(e.target.value); state.page = 0; load(); };

/* ---------- создание / изменение ---------- */

async function openMarineDialog(marine) {
  editingId = marine ? marine.id : null;
  $('dlgTitle').textContent = marine ? `Изменение десантника #${marine.id}` : 'Новый десантник';
  showErrors($('formErrors'), []);
  const form = $('marineForm');
  try {
    await fillChapterSelect(form.elements.chapterId, marine ? marine.chapter.id : null);
  } catch (err) {
    alert(err.message);
    return;
  }
  fillMarine(form, marine);
  $('marineDialog').showModal();
}

async function openEdit(id) {
  try {
    openMarineDialog(await api('GET', '/api/marines/' + id));
  } catch (err) {
    alert(err.message);
  }
}

$('btnCreate').onclick = () => openMarineDialog(null);
$('btnCancel').onclick = () => $('marineDialog').close();

$('marineForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const marine = readMarine(ev.target);
  const errs = validateMarine(marine);
  showErrors($('formErrors'), errs);
  if (errs.length) return;
  try {
    if (editingId) await api('PUT', '/api/marines/' + editingId, marine);
    else await api('POST', '/api/marines', marine);
    $('marineDialog').close();
    load();
  } catch (err) {
    showErrors($('formErrors'), err.messages);
  }
};

/* ---------- информация по ID ---------- */

async function openInfo(id) {
  try {
    const marine = await api('GET', '/api/marines/' + id);
    infoId = id;
    $('infoBody').innerHTML = marineCardHtml(marine);
    $('infoDialog').showModal();
  } catch (err) {
    alert(err.message);
  }
}

$('btnFind').onclick = () => {
  const id = Number($('idInput').value);
  if (!Number.isInteger(id) || id <= 0) { alert('Введите целый ID больше 0'); return; }
  openInfo(id);
};
$('idInput').onkeydown = (e) => { if (e.key === 'Enter') $('btnFind').click(); };
$('infoClose').onclick = () => $('infoDialog').close();
$('infoEdit').onclick = () => { $('infoDialog').close(); openEdit(infoId); };

/* ---------- удаление ---------- */

async function removeMarine(id) {
  if (!confirm(`Удалить десантника #${id}?`)) return;
  try {
    await api('DELETE', '/api/marines/' + id);
    load();
  } catch (err) {
    alert(err.message);
  }
}

/* ---------- старт ---------- */

renderMenu('index.html');
$('marineFields').innerHTML = marineFieldsHtml();
renderHeader();
load();
subscribeChanges(() => load()); // изменения других пользователей подтягиваются автоматически
