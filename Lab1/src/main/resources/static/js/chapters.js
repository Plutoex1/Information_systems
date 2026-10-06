'use strict';

const $ = id => document.getElementById(id);
let editingId = null;
let dissolvingId = null;
let hasMembers = false;

async function load() {
  try {
    const list = await api('GET', '/api/chapters');
    $('tbody').innerHTML = list.length === 0
      ? '<tr><td colspan="6" class="muted">Орденов пока нет</td></tr>'
      : list.map(c => `<tr>
          <td>${c.id}</td><td>${esc(c.name)}</td><td>${esc(c.parentLegion || '')}</td>
          <td>${c.marinesCount}</td><td>${esc(c.world)}</td>
          <td class="actions">
            <button data-act="members" data-id="${c.id}">Состав</button>
            <button data-act="edit" data-id="${c.id}">Изменить</button>
            <button data-act="dissolve" data-id="${c.id}" class="danger">Распустить</button>
          </td></tr>`).join('');
  } catch (err) {
    $('tbody').innerHTML = `<tr><td colspan="6" class="muted">${esc(err.message)}</td></tr>`;
  }
}

$('tbody').onclick = (ev) => {
  const btn = ev.target.closest('button[data-act]');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.act === 'members') showMembers(id);
  if (btn.dataset.act === 'edit') openEdit(id);
  if (btn.dataset.act === 'dissolve') openDissolve(id);
};

/* ---------- создание / изменение ---------- */

function openChapterDialog(chapter) {
  editingId = chapter ? chapter.id : null;
  $('dlgTitle').textContent = chapter ? `Изменение ордена #${chapter.id}` : 'Новый орден';
  const e = $('chapterForm').elements;
  e.name.value = chapter ? chapter.name : '';
  e.parentLegion.value = chapter ? (chapter.parentLegion || '') : '';
  e.marinesCount.value = chapter ? chapter.marinesCount : '';
  e.world.value = chapter ? chapter.world : '';
  showErrors($('formErrors'), []);
  $('chapterDialog').showModal();
}

async function openEdit(id) {
  try {
    openChapterDialog((await api('GET', '/api/chapters/' + id)).chapter);
  } catch (err) {
    alert(err.message);
  }
}

$('btnCreate').onclick = () => openChapterDialog(null);
$('btnCancel').onclick = () => $('chapterDialog').close();

$('chapterForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const e = ev.target.elements;
  const body = {
    name: e.name.value,
    parentLegion: e.parentLegion.value,
    marinesCount: num(e.marinesCount.value),
    world: e.world.value,
  };
  const errs = [];
  if (!body.name.trim()) errs.push('Название ордена не может быть пустым');
  if (body.marinesCount == null || !Number.isInteger(body.marinesCount)) errs.push('Численность должна быть целым числом');
  else if (body.marinesCount <= 0) errs.push('Численность должна быть больше 0');
  else if (body.marinesCount > 1000) errs.push('Численность не может превышать 1000');
  if (!body.world.trim()) errs.push('Мир обязателен');
  showErrors($('formErrors'), errs);
  if (errs.length) return;
  try {
    if (editingId) await api('PUT', '/api/chapters/' + editingId, body);
    else await api('POST', '/api/chapters', body);
    $('chapterDialog').close();
    load();
  } catch (err) {
    showErrors($('formErrors'), err.messages);
  }
};

/* ---------- состав ордена ---------- */

async function showMembers(id) {
  try {
    const d = await api('GET', '/api/chapters/' + id);
    $('membersBody').innerHTML = `<h2>Орден «${esc(d.chapter.name)}»</h2>${chapterCardHtml(d.chapter)}
      <h4>Десантники в системе (${d.marines.length})</h4>
      ${d.marines.length === 0 ? '<p class="muted">Нет связанных десантников</p>'
        : '<ul>' + d.marines.map(m => `<li>#${m.id} ${esc(m.name)} — ${m.category}, ${m.weaponType}</li>`).join('') + '</ul>'}`;
    $('membersDialog').showModal();
  } catch (err) {
    alert(err.message);
  }
}
$('membersClose').onclick = () => $('membersDialog').close();

/* ---------- роспуск ордена ---------- */

async function openDissolve(id) {
  try {
    const d = await api('GET', '/api/chapters/' + id);
    dissolvingId = id;
    hasMembers = d.marines.length > 0;
    $('dissolveTitle').textContent = `Распустить орден «${d.chapter.name}»?`;
    showErrors($('dissolveErrors'), []);
    $('targetLabel').style.display = hasMembers ? '' : 'none';
    $('dissolveOk').disabled = false;
    if (hasMembers) {
      $('dissolveText').textContent = `С орденом связано десантников: ${d.marines.length}. Выберите орден, в который они будут переведены.`;
      const others = await fillChapterSelect($('dissolveForm').elements.target, null, id);
      if (others.length === 0) {
        showErrors($('dissolveErrors'), ['Нет другого ордена для перевода десантников — сначала создайте его']);
        $('dissolveOk').disabled = true;
      }
    } else {
      $('dissolveText').textContent = 'С орденом нет связанных десантников. Орден будет удалён.';
    }
    $('dissolveDialog').showModal();
  } catch (err) {
    alert(err.message);
  }
}

$('dissolveCancel').onclick = () => $('dissolveDialog').close();

$('dissolveForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const target = hasMembers ? ev.target.elements.target.value : '';
  const url = '/api/chapters/' + dissolvingId + (target ? '?reassignTo=' + encodeURIComponent(target) : '');
  try {
    await api('DELETE', url);
    $('dissolveDialog').close();
    load();
  } catch (err) {
    showErrors($('dissolveErrors'), err.messages);
  }
};

/* ---------- старт ---------- */

renderMenu('chapters.html');
load();
subscribeChanges(() => load());
