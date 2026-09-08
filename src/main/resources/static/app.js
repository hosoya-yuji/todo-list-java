"use strict";
const labels = {
  OPEN: "未対応",
  IN_PROGRESS: "処理中",
  RESOLVED: "処理済み",
  DONE: "完了",
};
let board = { revision: 0, tasks: [] },
  view = "board",
  editing = null,
  editRevision = 0,
  busy = false,
  dragId = null;
const $ = (s) => document.querySelector(s);
const el = (tag, cls, text) => {
  const n = document.createElement(tag);
  if (cls) n.className = cls;
  if (text !== undefined) n.textContent = text;
  return n;
};
const children = (id) => board.tasks.filter((t) => t.parentId === id);
function notice(message, error = false) {
  const n = $("#notice");
  n.textContent = message;
  n.className = error ? "error" : "";
  n.hidden = false;
}
async function request(path, method = "GET", data) {
  const res = await fetch(path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: data === undefined ? undefined : JSON.stringify(data),
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok)
    throw new Error(
      json.message ||
        "保存できませんでした。接続を確認してやり直してください。",
    );
  return json;
}
async function refresh() {
  try {
    board = await request("/api/tasks");
    render();
  } catch (e) {
    notice(e.message, true);
  }
}
async function mutate(path, method, data, close = false) {
  if (busy) return;
  busy = true;
  document
    .querySelectorAll("#task-form button")
    .forEach((b) => (b.disabled = true));
  const previous = board;
  try {
    board = await request(path, method, data);
    if (close) $("#editor").close();
    render();
    const reopened = board.tasks.filter(
      (t) =>
        t.status === "IN_PROGRESS" &&
        previous.tasks.some((p) => p.id === t.id && p.status === "DONE"),
    );
    notice(
      reopened.length
        ? "保存しました。完了済みの親課題を「処理中」に戻しました。"
        : "保存しました。",
    );
  } catch (e) {
    if ($("#editor").open) {
      $("#form-error").textContent = e.message;
      $("#form-error").hidden = false;
    }
    notice(e.message, true);
    await refresh();
    if ($("#editor").open)
      $("#reload-edit").hidden = board.revision === editRevision;
  } finally {
    busy = false;
    document
      .querySelectorAll("#task-form button")
      .forEach((b) => (b.disabled = false));
  }
}
function matches(t) {
  const q = $("#search").value.trim().toLowerCase();
  return (
    !q ||
    t.title.toLowerCase().includes(q) ||
    ("TASK-" + t.id).toLowerCase().includes(q)
  );
}
function card(t) {
  const n = el("article", "card");
  n.dataset.id = t.id;
  n.draggable = !$("#search").value.trim();
  n.tabIndex = 0;
  n.setAttribute("aria-label", "TASK-" + t.id + " " + t.title + " 詳細を開く");
  const top = el("div", "card-top");
  top.append(el("span", "", "TASK-" + t.id), el("span", "", "⠿"));
  n.append(top, el("h3", "", t.title));
  if (t.parentId) {
    const p = board.tasks.find((p) => p.id === t.parentId);
    n.append(el("div", "parent", "↳ " + (p ? p.title : "親課題")));
  }
  const kids = children(t.id),
    done = kids.filter((k) => k.status === "DONE").length;
  if (kids.length) {
    const bar = el("div", "progress");
    const fill = el("span");
    fill.style.width = (done / kids.length) * 100 + "%";
    bar.append(fill);
    n.append(bar);
  }
  const footer = el("footer");
  footer.append(
    el(
      "span",
      "",
      "" +
        (kids.length
          ? "子課題 " + done + "/" + kids.length + " 完了"
          : t.parentId
            ? "子課題"
            : "課題"),
    ),
  );
  const today = new Date();
  const localDate =
    today.getFullYear() +
    "-" +
    String(today.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(today.getDate()).padStart(2, "0");
  footer.append(
    el(
      "span",
      t.dueDate && t.dueDate < localDate && t.status !== "DONE"
        ? "overdue"
        : "",
      t.dueDate || "期限なし",
    ),
  );
  n.append(footer);
  n.onclick = () => {
    if (!busy) openEditor(t);
  };
  n.onkeydown = (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      openEditor(t);
    }
  };
  n.ondragstart = (e) => {
    if (busy) {
      e.preventDefault();
      return;
    }
    dragId = t.id;
    e.dataTransfer.setData("text/plain", String(t.id));
    e.dataTransfer.effectAllowed = "move";
    n.classList.add("dragging");
  };
  n.ondragend = () => {
    dragId = null;
    clearDrop();
    n.classList.remove("dragging");
  };
  return n;
}
function clearDrop() {
  document
    .querySelectorAll(".drop-target,.drop-before")
    .forEach((n) => n.classList.remove("drop-target", "drop-before"));
}
function render() {
  $("#board").hidden = view !== "board";
  $("#list").hidden = view !== "list";
  $("#view-title").textContent = view === "board" ? "ボード" : "課題一覧";
  $(".subtitle").textContent =
    view === "board"
      ? "課題を動かして、進捗を整えましょう。"
      : "親子のつながりを見ながら、作業を整理しましょう。";
  $("#board-tab").classList.toggle("active", view === "board");
  $("#list-tab").classList.toggle("active", view === "list");
  $("#board-tab").setAttribute("aria-pressed", view === "board");
  $("#list-tab").setAttribute("aria-pressed", view === "list");
  $("#summary").textContent =
    board.tasks.length +
    " 件の課題 ・ " +
    board.tasks.filter((t) => t.status === "DONE").length +
    " 件完了";
  $("#hint").textContent = $("#search").value.trim()
    ? "検索中はドラッグ操作を停止しています。課題の詳細から状態を変更できます。"
    : "カードをドラッグして状態・順番を変更できます。クリックすると詳細を開きます。";
  if (view === "list")
    $("#hint").textContent =
      "矢印で子課題を展開できます。課題名をクリックすると詳細を開きます。";
  $("#board").replaceChildren();
  for (const [status, label] of Object.entries(labels)) {
    const col = el("section", "column " + status),
      all = board.tasks.filter((t) => t.status === status && matches(t)),
      heading = el("h2");
    heading.append(
      el("span", "dot"),
      el("span", "", label),
      el("span", "count", all.length),
    );
    col.append(heading);
    const cards = el("div", "cards");
    for (const t of all) cards.append(card(t));
    if (!all.length) cards.append(el("p", "empty", "課題はありません"));
    col.append(cards);
    const add = el("button", "column-add", "＋ 課題を追加");
    add.onclick = () => openEditor(null, null, status);
    col.append(add);
    function target(e) {
      const candidates = [...cards.querySelectorAll(".card")].filter(
        (n) => Number(n.dataset.id) !== dragId,
      );
      return candidates.find(
        (n) =>
          e.clientY <
          n.getBoundingClientRect().top + n.getBoundingClientRect().height / 2,
      );
    }
    col.ondragover = (e) => {
      if (dragId === null || busy) return;
      e.preventDefault();
      e.dataTransfer.dropEffect = "move";
      clearDrop();
      col.classList.add("drop-target");
      target(e)?.classList.add("drop-before");
    };
    col.ondrop = (e) => {
      e.preventDefault();
      if (dragId === null || busy) return;
      const id = dragId,
        before = target(e);
      clearDrop();
      dragId = null;
      mutate("/api/tasks/" + id + "/position", "PATCH", {
        status,
        beforeId: before ? Number(before.dataset.id) : null,
        revision: board.revision,
      });
    };
    $("#board").append(col);
  }
  renderList();
}
function listRow(t, child = false) {
  const row = el("div", "list-row" + (child ? " child-row" : ""));
  row.append(el("small", "", "TASK-" + t.id));
  const button = el("button", "task-name", t.title);
  button.onclick = (e) => {
    e.preventDefault();
    openEditor(t);
  };
  row.append(
    button,
    el("span", "status-label", labels[t.status]),
    el("small", "", t.dueDate || "期限なし"),
  );
  return row;
}
function renderList() {
  const root = $("#list");
  root.replaceChildren();
  for (const t of board.tasks.filter((t) => !t.parentId)) {
    const kids = children(t.id);
    if (!matches(t) && !kids.some(matches)) continue;
    if (kids.length) {
      const detail = el("section");
      const group = el("div");
      group.id = "child-group-" + t.id;
      group.hidden = !$("#search").value;
      const toggle = el("button", "expand-children", group.hidden ? "▸" : "▾");
      toggle.setAttribute("aria-label", t.title + "の子課題を展開");
      toggle.setAttribute("aria-controls", group.id);
      toggle.setAttribute("aria-expanded", !group.hidden);
      toggle.onclick = () => {
        group.hidden = !group.hidden;
        toggle.textContent = group.hidden ? "▸" : "▾";
        toggle.setAttribute("aria-expanded", !group.hidden);
      };
      const row = listRow(t);
      row.prepend(toggle);
      row.append(
        el(
          "small",
          "",
          "子課題 " +
            kids.filter((k) => k.status === "DONE").length +
            "/" +
            kids.length,
        ),
      );
      detail.append(row, group);
      for (const k of kids) group.append(listRow(k, true));
      root.append(detail);
    } else root.append(listRow(t));
  }
  if (!root.children.length)
    root.append(
      el(
        "p",
        "empty",
        "該当する課題はありません。課題を追加して始めましょう。",
      ),
    );
}
function openEditor(t = null, parentId = null, status = "OPEN") {
  if (busy) return;
  editing = t?.id ?? null;
  editRevision = board.revision;
  const form = $("#task-form");
  form.reset();
  $("#form-error").hidden = true;
  $("#reload-edit").hidden = true;
  $("#dialog-title").textContent = t ? "課題の詳細" : "課題を追加";
  $("#task-key").textContent = t ? "TASK-" + t.id : "NEW TASK";
  form.elements.title.value = t?.title ?? "";
  form.elements.description.value = t?.description ?? "";
  form.elements.dueDate.value = t?.dueDate ?? "";
  form.elements.status.value = t?.status ?? status;
  const select = form.elements.parentId;
  select.replaceChildren();
  const none = el("option", "", "親課題なし");
  none.value = "";
  select.append(none);
  const hasChildren = t && children(t.id).length > 0;
  for (const p of board.tasks.filter(
    (p) => !p.parentId && p.id !== editing && !hasChildren,
  )) {
    const o = el("option", "", "TASK-" + p.id + " " + p.title);
    o.value = p.id;
    select.append(o);
  }
  select.value = t?.parentId ?? parentId ?? "";
  select.disabled = !!hasChildren;
  $("#delete").hidden = !t;
  $("#children-section").hidden = !t || !!t.parentId;
  $("#children").replaceChildren();
  if (t) {
    const kids = children(t.id);
    $("#children-count").textContent =
      kids.filter((k) => k.status === "DONE").length +
      "/" +
      kids.length +
      " 完了";
    for (const k of kids) {
      const b = el("button", "", labels[k.status] + "　" + k.title);
      b.type = "button";
      b.onclick = () => {
        if (confirm("現在の未保存の入力を破棄して子課題を開きますか？"))
          openEditor(k);
      };
      $("#children").append(b);
    }
  }
  if (!$("#editor").open) $("#editor").showModal();
  form.elements.title.focus();
}
$("#task-form").onsubmit = (e) => {
  e.preventDefault();
  const f = e.target;
  mutate(
    "/api/tasks" + (editing ? "/" + editing : ""),
    editing ? "PUT" : "POST",
    {
      title: f.elements.title.value,
      description: f.elements.description.value,
      dueDate: f.elements.dueDate.value || null,
      status: f.elements.status.value,
      parentId: f.elements.parentId.value
        ? Number(f.elements.parentId.value)
        : null,
      revision: editRevision,
    },
    true,
  );
};
$("#add").onclick = () => openEditor();
$("#reload-edit").onclick = () => {
  const current = board.tasks.find((t) => t.id === editing);
  if (editing && !current) {
    $("#editor").close();
    notice("この課題は別の画面で削除されました。", true);
  } else openEditor(current ?? null);
};
$("#add-child").onclick = () => {
  if (confirm("現在の未保存の入力を破棄して子課題を追加しますか？"))
    openEditor(null, editing);
};
$("#delete").onclick = () => {
  if (confirm("この課題を削除しますか？"))
    mutate(
      "/api/tasks/" + editing + "?revision=" + editRevision,
      "DELETE",
      undefined,
      true,
    );
};
$("#close").onclick = $("#cancel").onclick = () => $("#editor").close();
$("#editor").addEventListener("cancel", (e) => {
  if (busy) e.preventDefault();
});
$("#board-tab").onclick = () => {
  view = "board";
  render();
};
$("#list-tab").onclick = () => {
  view = "list";
  render();
};
$("#search").oninput = render;
$("#refresh").onclick = refresh;
refresh();
