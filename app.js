// Gestor de Atas & Prazos - Lógica Web em JavaScript

// Estrutura de Estado
let atas = JSON.parse(localStorage.getItem('gestor_atas_list') || '[]');
let decisions = JSON.parse(localStorage.getItem('gestor_decisions_list') || '[]');
let currentFilter = 'ALL';
let currentPreviewDecisions = [];
let currentPreviewAta = null;

// Inicialização
document.addEventListener('DOMContentLoaded', () => {
  // Configurar data padrão de hoje no input
  const todayStr = new Date().toISOString().split('T')[0];
  const dateInput = document.getElementById('ataDate');
  if (dateInput) dateInput.value = todayStr;

  // Se não houver dados, adicionar dados de exemplo para demonstração
  if (atas.length === 0 && decisions.length === 0) {
    seedInitialData();
  }

  // Configurar clique na zona de upload de arquivo
  const dropZone = document.getElementById('dropZone');
  const fileInput = document.getElementById('ataFileInput');
  if (dropZone && fileInput) {
    dropZone.addEventListener('click', (e) => {
      if (e.target !== fileInput) {
        fileInput.click();
      }
    });
  }

  updateDashboard();
  renderTasks();
  renderAtas();
  lucide.createIcons();
});

// Handlers de Upload de Arquivo (Drag & Drop e Input)
function handleDragOver(e) {
  e.preventDefault();
  e.stopPropagation();
  const dropZone = document.getElementById('dropZone');
  if (dropZone) dropZone.classList.add('border-indigo-600', 'bg-indigo-50/50');
}

function handleDragLeave(e) {
  e.preventDefault();
  e.stopPropagation();
  const dropZone = document.getElementById('dropZone');
  if (dropZone) dropZone.classList.remove('border-indigo-600', 'bg-indigo-50/50');
}

function handleFileDrop(e) {
  e.preventDefault();
  e.stopPropagation();
  const dropZone = document.getElementById('dropZone');
  if (dropZone) dropZone.classList.remove('border-indigo-600', 'bg-indigo-50/50');

  const files = e.dataTransfer?.files;
  if (files && files.length > 0) {
    readFileContent(files[0]);
  }
}

function handleFileSelect(e) {
  const files = e.target.files;
  if (files && files.length > 0) {
    readFileContent(files[0]);
  }
}

async function readFileContent(file) {
  const statusEl = document.getElementById('fileUploadStatus');
  const nameEl = document.getElementById('fileNameDisplay');
  const contentTextarea = document.getElementById('ataContent');
  const titleInput = document.getElementById('ataTitle');
  const dateInput = document.getElementById('ataDate');

  if (statusEl && nameEl) {
    nameEl.textContent = `${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
    statusEl.className = 'text-xs font-bold text-indigo-700 bg-indigo-100 px-3 py-1 rounded-full flex items-center gap-1.5';
    statusEl.classList.remove('hidden');
  }

  // Preencher título sugerido a partir do nome do arquivo
  if (titleInput && (!titleInput.value || titleInput.value.trim() === '')) {
    const cleanName = file.name.replace(/\.[^/.]+$/, "").replace(/[-_]/g, ' ');
    titleInput.value = cleanName.charAt(0).toUpperCase() + cleanName.slice(1);
  }

  const fileNameLower = file.name.toLowerCase();

  // 1. Processamento de PDF
  if (fileNameLower.endsWith('.pdf') || file.type === 'application/pdf') {
    if (contentTextarea) {
      contentTextarea.value = "Lendo e interpretando documento PDF...";
    }

    try {
      if (typeof window.pdfjsLib === 'undefined') {
        throw new Error("Biblioteca de PDF não carregou.");
      }

      // Configurar worker
      if (window.pdfjsLib.GlobalWorkerOptions) {
        window.pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.worker.min.js';
      }

      const arrayBuffer = await file.arrayBuffer();
      const typedArray = new Uint8Array(arrayBuffer);
      const loadingTask = window.pdfjsLib.getDocument({
        data: typedArray,
        useSystemFonts: true,
        disableFontFace: true
      });
      
      const pdf = await loadingTask.promise;
      let extractedPages = [];
      const totalPages = Math.min(pdf.numPages, 40);

      for (let i = 1; i <= totalPages; i++) {
        const page = await pdf.getPage(i);
        const textContent = await page.getTextContent();
        
        // Reconstruir linhas preservando quebras de linha pela posição vertical Y
        let lastY = null;
        let pageLines = [];
        let currentLine = '';

        for (const item of textContent.items) {
          const itemY = item.transform ? item.transform[5] : null;
          const itemStr = item.str;

          if (lastY !== null && itemY !== null && Math.abs(itemY - lastY) > 4) {
            if (currentLine.trim()) {
              pageLines.push(currentLine.trim());
            }
            currentLine = itemStr;
          } else {
            if (currentLine.length > 0 && !currentLine.endsWith(' ') && !itemStr.startsWith(' ')) {
              currentLine += ' ';
            }
            currentLine += itemStr;
          }
          lastY = itemY;
        }

        if (currentLine.trim()) {
          pageLines.push(currentLine.trim());
        }

        const pageText = pageLines.join('\n');
        if (pageText.trim()) {
          extractedPages.push(pageText.trim());
        }
      }

      const fullText = extractedPages.join('\n\n');

      if (fullText.trim().length > 10) {
        if (contentTextarea) contentTextarea.value = fullText;
        if (statusEl && nameEl) {
          statusEl.className = 'text-xs font-bold text-emerald-700 bg-emerald-100 px-3 py-1 rounded-full flex items-center gap-1.5';
          nameEl.textContent = `${file.name} - Texto extraído com sucesso (${totalPages} pág.)`;
        }

        // Tentar detectar data no texto
        autoDetectDateAndTitle(fullText, file.name);

        // Auto-interpretar e já mostrar a pré-visualização com 15 dias padrão
        autoTriggerInterpretation();
      } else {
        if (contentTextarea) {
          contentTextarea.value = "";
          contentTextarea.placeholder = "Aviso: Este PDF parece ser escaneado como imagem. Digite ou cole o texto da ata aqui...";
        }
        if (statusEl && nameEl) {
          statusEl.className = 'text-xs font-bold text-amber-700 bg-amber-100 px-3 py-1 rounded-full flex items-center gap-1.5';
          nameEl.textContent = `${file.name} (PDF em imagem - digite ou cole o texto abaixo)`;
        }
      }
    } catch (err) {
      console.error("Erro ao ler PDF:", err);
      if (contentTextarea) {
        contentTextarea.value = "";
        contentTextarea.placeholder = "Por favor, copie e cole o texto da ata aqui no campo abaixo...";
      }
      if (statusEl && nameEl) {
        statusEl.className = 'text-xs font-bold text-amber-700 bg-amber-100 px-3 py-1 rounded-full flex items-center gap-1.5';
        nameEl.textContent = `${file.name} (Cole o texto no campo abaixo)`;
      }
    }
    return;
  }

  // 2. Processamento de DOCX (Word)
  if (fileNameLower.endsWith('.docx')) {
    if (contentTextarea) {
      contentTextarea.value = "Extraindo texto do Word...";
    }
    try {
      const arrayBuffer = await file.arrayBuffer();
      const result = await window.mammoth.extractRawText({ arrayBuffer });
      const fullText = result.value.trim();
      if (contentTextarea) {
        contentTextarea.value = fullText;
        if (statusEl && nameEl) {
          statusEl.className = 'text-xs font-bold text-emerald-700 bg-emerald-100 px-3 py-1 rounded-full flex items-center gap-1.5';
          nameEl.textContent = `${file.name} - Texto extraído com sucesso`;
        }
        autoDetectDateAndTitle(fullText, file.name);
        autoTriggerInterpretation();
      }
    } catch (err) {
      console.error("Erro ao ler DOCX:", err);
    }
    return;
  }

  // 3. Arquivos de texto (.txt, .md, .csv)
  const reader = new FileReader();
  reader.onload = function(event) {
    const content = event.target?.result;
    if (typeof content === 'string' && contentTextarea) {
      contentTextarea.value = content;
      if (statusEl && nameEl) {
        statusEl.className = 'text-xs font-bold text-emerald-700 bg-emerald-100 px-3 py-1 rounded-full flex items-center gap-1.5';
        nameEl.textContent = `${file.name} - Carregado com sucesso`;
      }
      autoDetectDateAndTitle(content, file.name);
      autoTriggerInterpretation();
    }
  };
  reader.readAsText(file, 'UTF-8');
}

// Tentar identificar data no texto da ata
function autoDetectDateAndTitle(text, fileName) {
  const dateInput = document.getElementById('ataDate');
  const titleInput = document.getElementById('ataTitle');

  // Buscar datas no formato DD/MM/AAAA ou DD de Mês de AAAA
  const dateMatch1 = text.match(/(\d{1,2})[\/\.-](\d{1,2})[\/\.-](\d{4})/);
  if (dateMatch1 && dateInput) {
    const day = dateMatch1[1].padStart(2, '0');
    const month = dateMatch1[2].padStart(2, '0');
    const year = dateMatch1[3];
    dateInput.value = `${year}-${month}-${day}`;
  } else {
    // Ex: 22 de agosto de 2026
    const months = {
      'janeiro': '01', 'fevereiro': '02', 'março': '03', 'marco': '03', 'abril': '04',
      'maio': '05', 'junho': '06', 'julho': '07', 'agosto': '08',
      'setembro': '09', 'outubro': '10', 'novembro': '11', 'dezembro': '12'
    };
    const dateMatch2 = text.match(/(\d{1,2})\s+de\s+([a-zA-Zç]+)\s+de\s+(\d{4})/i);
    if (dateMatch2 && dateInput) {
      const day = dateMatch2[1].padStart(2, '0');
      const mName = dateMatch2[2].toLowerCase();
      const month = months[mName] || '01';
      const year = dateMatch2[3];
      dateInput.value = `${year}-${month}-${day}`;
    }
  }

  // Se o título for genérico ou vazio, criar um título contextual
  if (titleInput && (!titleInput.value || titleInput.value.includes('Ata') || titleInput.value.trim() === '')) {
    if (dateInput && dateInput.value) {
      const parts = dateInput.value.split('-');
      titleInput.value = `Ata de Reunião - ${parts[2]}/${parts[1]}/${parts[0]}`;
    }
  }
}

// Auto-disparar interpretação para abrir a tabela de prazos imediatamente
function autoTriggerInterpretation() {
  setTimeout(() => {
    const titleInput = document.getElementById('ataTitle');
    const dateInput = document.getElementById('ataDate');
    const contentTextarea = document.getElementById('ataContent');

    if (!contentTextarea || !contentTextarea.value.trim()) return;

    const title = (titleInput && titleInput.value.trim()) || "Ata de Reunião";
    const date = (dateInput && dateInput.value) || new Date().toISOString().split('T')[0];
    const content = contentTextarea.value.trim();

    const ataId = 'ata_' + Date.now();
    currentPreviewAta = {
      id: ataId,
      title,
      date,
      location: '',
      content,
      createdAt: new Date().toISOString()
    };

    currentPreviewDecisions = extractDecisionsFromText(content, title, ataId, date);

    const previewSection = document.getElementById('previewSection');
    if (previewSection) {
      previewSection.classList.remove('hidden');
      renderPreviewTable();
      previewSection.scrollIntoView({ behavior: 'smooth' });
    }
  }, 100);
}

// Alternar abas de navegação
function switchTab(tab) {
  const views = ['alerts', 'atas', 'upload'];
  views.forEach(v => {
    const viewEl = document.getElementById(`view-${v}`);
    const btnEl = document.getElementById(`tab${capitalize(v)}Btn`);
    if (v === tab) {
      viewEl.classList.remove('hidden');
      btnEl.className = "tab-btn px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-semibold flex items-center gap-1.5 transition-all bg-white text-indigo-600 shadow-sm";
    } else {
      viewEl.classList.add('hidden');
      btnEl.className = "tab-btn px-3.5 py-1.5 rounded-lg text-xs sm:text-sm font-semibold flex items-center gap-1.5 transition-all text-slate-600 hover:text-slate-900";
    }
  });
  lucide.createIcons();
}

function capitalize(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// Filtros
function setFilter(filter) {
  currentFilter = filter;
  const filters = ['ALL', 'OVERDUE', 'TODAY', 'FOLLOWUP', 'PERMANENT', 'COMPLETED'];
  filters.forEach(f => {
    const btn = document.getElementById(`filterBtn-${f}`);
    if (btn) {
      if (f === filter) {
        btn.className = "filter-pill px-3 py-1 text-xs font-bold rounded-lg bg-slate-900 text-white transition";
      } else {
        btn.className = "filter-pill px-3 py-1 text-xs font-semibold rounded-lg bg-slate-100 text-slate-700 hover:bg-slate-200 transition";
      }
    }
  });
  renderTasks();
}

// Atualizar estatísticas no topo
function updateDashboard() {
  const now = new Date();
  const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const tomorrowMidnight = todayMidnight + 86400000;

  let overdueCount = 0;
  let todayCount = 0;
  let upcomingCount = 0;

  decisions.forEach(d => {
    if (d.completed || d.decisionType === 'PERMANENT_PROCEDURE') return;
    const dueTime = new Date(d.dueDate).getTime();
    if (dueTime < todayMidnight) {
      overdueCount++;
    } else if (dueTime >= todayMidnight && dueTime < tomorrowMidnight) {
      todayCount++;
    } else {
      upcomingCount++;
    }
  });

  document.getElementById('statOverdue').textContent = overdueCount;
  document.getElementById('statToday').textContent = todayCount;
  document.getElementById('statUpcoming').textContent = upcomingCount;
  document.getElementById('alertCountBadge').textContent = overdueCount + todayCount;
}

// Renderizar Tarefas / Deliberações
function renderTasks() {
  const container = document.getElementById('tasksContainer');
  const search = (document.getElementById('searchResponsible')?.value || '').toLowerCase();

  const now = new Date();
  const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const tomorrowMidnight = todayMidnight + 86400000;

  const filtered = decisions.filter(d => {
    // Filtro por texto
    const matchesSearch = (d.responsible || '').toLowerCase().includes(search) ||
                          (d.action || '').toLowerCase().includes(search) ||
                          (d.typeLabel || '').toLowerCase().includes(search) ||
                          (d.ataTitle || '').toLowerCase().includes(search);
    if (!matchesSearch) return false;

    // Filtro por categoria
    if (currentFilter === 'COMPLETED') return d.completed;
    if (currentFilter === 'PERMANENT') return d.decisionType === 'PERMANENT_PROCEDURE';
    if (currentFilter === 'FOLLOWUP') return d.decisionType === 'FOLLOW_UP_ASSIGNMENT';

    // Se estiver no filtro 'Todas' (ALL), exibir tanto pendentes quanto concluídas para manter a visibilidade do histórico
    if (currentFilter === 'ALL') {
      return true;
    }

    // Para filtros estritos de alerta temporal (Vencidas / Hoje), ignorar tarefas já concluídas ou regras permanentes
    if (d.completed) return false;
    if (d.decisionType === 'PERMANENT_PROCEDURE') return false;

    const dueTime = new Date(d.dueDate).getTime();
    if (currentFilter === 'OVERDUE') return dueTime < todayMidnight;
    if (currentFilter === 'TODAY') return dueTime >= todayMidnight && dueTime < tomorrowMidnight;
    return true;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="bg-white border border-slate-200 rounded-2xl p-8 text-center text-slate-400">
        <i data-lucide="check-check" class="w-10 h-10 mx-auto text-slate-300 mb-2"></i>
        <p class="text-sm font-medium text-slate-600">Nenhuma deliberação encontrada para este filtro.</p>
        <p class="text-xs text-slate-400 mt-1">Cadastre novas atas ou altere os termos da busca.</p>
      </div>
    `;
    lucide.createIcons();
    return;
  }

  container.innerHTML = filtered.map(d => {
    const isPermanent = d.decisionType === 'PERMANENT_PROCEDURE';
    const isFollowUp = d.decisionType === 'FOLLOW_UP_ASSIGNMENT';
    const dueTime = new Date(d.dueDate).getTime();

    let badgeClass = "bg-indigo-50 text-indigo-700 border-indigo-200";
    let badgeText = `Prazo: ${formatDate(d.dueDate)}`;
    let cardBorder = "border-slate-200";

    // Type Badge
    let typeBadgeHtml = '';
    if (isPermanent) {
      typeBadgeHtml = `<span class="text-[11px] px-2 py-0.5 rounded-md font-bold bg-purple-50 text-purple-700 border border-purple-200">Decisão permanente de procedimento</span>`;
      badgeClass = "bg-purple-100/70 text-purple-800 border-purple-200";
      badgeText = "Regra Permanente";
    } else if (isFollowUp) {
      typeBadgeHtml = `<span class="text-[11px] px-2 py-0.5 rounded-md font-bold bg-teal-50 text-teal-700 border border-teal-200">Atribuição de acompanhamento</span>`;
      badgeText = `Prazo: ${formatDate(d.dueDate)} (15d padrão)`;
    } else {
      typeBadgeHtml = `<span class="text-[11px] px-2 py-0.5 rounded-md font-bold bg-sky-50 text-sky-700 border border-sky-200">Ação com prazo</span>`;
    }

    if (d.completed) {
      badgeClass = "bg-emerald-50 text-emerald-700 border-emerald-200";
      badgeText = "Concluída";
      cardBorder = "border-emerald-100 bg-emerald-50/20";
    } else if (!isPermanent) {
      if (dueTime < todayMidnight) {
        badgeClass = "bg-red-100 text-red-800 border-red-200 font-bold";
        badgeText = `⚠️ Atrasada (${formatDate(d.dueDate)})`;
        cardBorder = "border-red-200 bg-red-50/30";
      } else if (dueTime >= todayMidnight && dueTime < tomorrowMidnight) {
        badgeClass = "bg-amber-100 text-amber-900 border-amber-200 font-bold";
        badgeText = `⏰ Vence Hoje (${formatDate(d.dueDate)})`;
        cardBorder = "border-amber-200 bg-amber-50/30";
      }
    }

    return `
      <div class="bg-white border ${cardBorder} rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 transition hover:shadow-sm">
        <div class="flex items-start gap-3.5 flex-1">
          <input type="checkbox" ${d.completed ? 'checked' : ''} onchange="toggleTaskComplete('${d.id}')" class="mt-1 w-5 h-5 text-indigo-600 rounded-lg border-slate-300 focus:ring-indigo-500 cursor-pointer">
          <div class="space-y-1.5 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              ${typeBadgeHtml}
              <span class="text-xs px-2.5 py-0.5 rounded-md border font-medium ${badgeClass}">${badgeText}</span>
              <span class="text-xs font-semibold text-slate-400">• Ata: ${escapeHtml(d.ataTitle)}</span>
            </div>
            <h3 class="text-sm font-bold ${d.completed ? 'line-through text-slate-400' : 'text-slate-900'}">${escapeHtml(d.action)}</h3>
            <p class="text-xs font-medium text-slate-500 flex items-center gap-1">
              <i data-lucide="user" class="w-3.5 h-3.5 text-slate-400"></i> Responsável(is): <strong class="text-slate-700">${escapeHtml(d.responsible)}</strong>
            </p>
          </div>
        </div>
        <div class="flex items-center gap-2 self-end sm:self-center">
          <button onclick="deleteDecision('${d.id}')" title="Remover deliberação" class="p-2 text-slate-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition">
            <i data-lucide="trash-2" class="w-4 h-4"></i>
          </button>
        </div>
      </div>
    `;
  }).join('');

  lucide.createIcons();
}

// Renderizar Histórico de Atas
function renderAtas() {
  const container = document.getElementById('atasListContainer');
  if (atas.length === 0) {
    container.innerHTML = `
      <div class="col-span-2 bg-white border border-slate-200 rounded-2xl p-8 text-center text-slate-400">
        <i data-lucide="folder-open" class="w-10 h-10 mx-auto text-slate-300 mb-2"></i>
        <p class="text-sm font-medium text-slate-600">Nenhuma ata cadastrada ainda.</p>
      </div>
    `;
    lucide.createIcons();
    return;
  }

  container.innerHTML = atas.map(a => {
    const relatedDecisions = decisions.filter(d => d.ataId === a.id);
    const completedCount = relatedDecisions.filter(d => d.completed).length;

    return `
      <div class="bg-white border border-slate-200 rounded-2xl p-5 space-y-3 transition hover:border-slate-300">
        <div class="flex items-start justify-between">
          <div>
            <span class="text-xs font-semibold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">${formatDate(a.date)}</span>
            <h3 class="text-base font-bold text-slate-900 mt-1">${escapeHtml(a.title)}</h3>
            <p class="text-xs text-slate-400">${escapeHtml(a.location || 'Sem local especificado')}</p>
          </div>
          <button onclick="deleteAta('${a.id}')" title="Excluir ata" class="p-1.5 text-slate-400 hover:text-red-600 rounded-lg hover:bg-red-50">
            <i data-lucide="trash" class="w-4 h-4"></i>
          </button>
        </div>

        <p class="text-xs text-slate-600 line-clamp-3 bg-slate-50 p-2.5 rounded-xl border border-slate-100 font-mono">${escapeHtml(a.content)}</p>

        <div class="pt-2 border-t border-slate-100 flex items-center justify-between text-xs font-medium text-slate-500">
          <span>${relatedDecisions.length} deliberações registradas</span>
          <span class="text-emerald-600 font-semibold">${completedCount}/${relatedDecisions.length} concluídas</span>
        </div>
      </div>
    `;
  }).join('');

  lucide.createIcons();
}

// Processar e Interpretar Ata (Mostra Pré-visualização com 15 dias padrão)
function handleInterpretSubmit(e) {
  e.preventDefault();

  const title = document.getElementById('ataTitle').value.trim();
  const date = document.getElementById('ataDate').value;
  const content = document.getElementById('ataContent').value.trim();

  if (!content) {
    alert('Por favor, informe ou cole o conteúdo da ata.');
    return;
  }

  const ataId = 'ata_' + Date.now();
  currentPreviewAta = {
    id: ataId,
    title,
    date,
    location: '',
    content,
    createdAt: new Date().toISOString()
  };

  currentPreviewDecisions = extractDecisionsFromText(content, title, ataId, date);

  if (currentPreviewDecisions.length === 0) {
    // Se não encontrou linhas estruturadas, cria uma decisão geral de acompanhamento com 15 dias
    const baseDate = new Date(date || new Date());
    const dueDate = new Date(baseDate.getTime() + 15 * 86400000).toISOString().split('T')[0];
    currentPreviewDecisions.push({
      id: 'dec_' + Date.now() + '_0',
      ataId,
      ataTitle: title,
      action: content.substring(0, 120) + (content.length > 120 ? '...' : ''),
      responsible: 'Comissão / Designados',
      dueDate,
      decisionType: 'FOLLOW_UP_ASSIGNMENT',
      typeLabel: 'Atribuição de acompanhamento',
      days: 15,
      completed: false
    });
  }

  const previewSection = document.getElementById('previewSection');
  if (previewSection) {
    previewSection.classList.remove('hidden');
    renderPreviewTable();
    previewSection.scrollIntoView({ behavior: 'smooth' });
  }
}

// Renderiza a Tabela de Pré-visualização com Ajustes Rápidos
function renderPreviewTable() {
  const tbody = document.getElementById('previewTableBody');
  if (!tbody) return;

  if (currentPreviewDecisions.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="py-6 text-center text-slate-400 font-medium">
          Nenhuma decisão na lista. Clique em "+ Adicionar Item" para criar uma manualmente.
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = currentPreviewDecisions.map((item, idx) => {
    const isPermanent = item.decisionType === 'PERMANENT_PROCEDURE';
    const isFollowUp = item.decisionType === 'FOLLOW_UP_ASSIGNMENT';

    return `
      <tr class="hover:bg-slate-50/70 transition">
        <td class="py-3 px-3 text-center font-bold text-slate-400 align-top">${idx + 1}</td>
        
        <!-- Ação / Descrição -->
        <td class="py-3 px-4 align-top">
          <input type="text" value="${escapeHtml(item.action)}" onchange="updatePreviewAction(${idx}, this.value)" class="w-full px-2.5 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:bg-white focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 font-medium text-slate-900">
        </td>

        <!-- Responsável(is) -->
        <td class="py-3 px-4 align-top">
          <input type="text" value="${escapeHtml(item.responsible)}" onchange="updatePreviewResponsible(${idx}, this.value)" placeholder="Ex: Edvaldo, Reginaldo" class="w-full px-2.5 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:bg-white focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 font-semibold text-slate-800">
        </td>

        <!-- Tipo e Prazos Rápidos -->
        <td class="py-3 px-4 align-top space-y-2">
          <div class="flex items-center gap-1.5">
            <span class="text-[11px] font-bold px-2 py-0.5 rounded-md ${
              isPermanent ? 'bg-purple-100 text-purple-800' :
              isFollowUp ? 'bg-teal-100 text-teal-800' : 'bg-sky-100 text-sky-800'
            }">
              ${escapeHtml(item.typeLabel)}
            </span>
            <span class="text-xs font-semibold text-slate-500">
              ${isPermanent ? '• Sem expiração' : `• Data: ${formatDate(item.dueDate)}`}
            </span>
          </div>

          <!-- Botões de ajuste rápido de prazo -->
          <div class="flex flex-wrap items-center gap-1">
            <button type="button" onclick="setPreviewDeadline(${idx}, 7)" class="px-2 py-0.5 rounded text-[10px] font-bold ${item.days === 7 && !isPermanent ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'} transition">
              7d
            </button>
            <button type="button" onclick="setPreviewDeadline(${idx}, 15)" title="Prazo padrão para atribuições de acompanhamento" class="px-2 py-0.5 rounded text-[10px] font-bold ${isFollowUp || (item.days === 15 && !isPermanent) ? 'bg-teal-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'} transition">
              15d (Padrão)
            </button>
            <button type="button" onclick="setPreviewDeadline(${idx}, 30)" class="px-2 py-0.5 rounded text-[10px] font-bold ${item.days === 30 && !isPermanent ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'} transition">
              30d
            </button>
            <button type="button" onclick="setPreviewDeadline(${idx}, 0)" class="px-2 py-0.5 rounded text-[10px] font-bold ${isPermanent ? 'bg-purple-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'} transition">
              Permanente
            </button>
          </div>
        </td>

        <!-- Ação Remover -->
        <td class="py-3 px-3 text-center align-top">
          <button type="button" onclick="removePreviewItem(${idx})" title="Remover item" class="p-1.5 text-slate-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition">
            <i data-lucide="trash-2" class="w-4 h-4"></i>
          </button>
        </td>
      </tr>
    `;
  }).join('');

  lucide.createIcons();
}

function setPreviewDeadline(idx, days) {
  if (!currentPreviewDecisions[idx] || !currentPreviewAta) return;
  const baseDate = new Date(currentPreviewAta.date || new Date());

  if (days === 0) {
    currentPreviewDecisions[idx].decisionType = 'PERMANENT_PROCEDURE';
    currentPreviewDecisions[idx].typeLabel = 'Decisão permanente de procedimento';
    currentPreviewDecisions[idx].days = 0;
    currentPreviewDecisions[idx].dueDate = new Date(baseDate.getTime() + 3650 * 86400000).toISOString().split('T')[0];
  } else if (days === 15) {
    currentPreviewDecisions[idx].decisionType = 'FOLLOW_UP_ASSIGNMENT';
    currentPreviewDecisions[idx].typeLabel = 'Atribuição de acompanhamento';
    currentPreviewDecisions[idx].days = 15;
    currentPreviewDecisions[idx].dueDate = new Date(baseDate.getTime() + 15 * 86400000).toISOString().split('T')[0];
  } else {
    currentPreviewDecisions[idx].decisionType = 'ACTION_DEADLINE';
    currentPreviewDecisions[idx].typeLabel = 'Ação com prazo';
    currentPreviewDecisions[idx].days = days;
    currentPreviewDecisions[idx].dueDate = new Date(baseDate.getTime() + days * 86400000).toISOString().split('T')[0];
  }

  renderPreviewTable();
}

function updatePreviewAction(idx, val) {
  if (currentPreviewDecisions[idx]) {
    currentPreviewDecisions[idx].action = val.trim();
  }
}

function updatePreviewResponsible(idx, val) {
  if (currentPreviewDecisions[idx]) {
    currentPreviewDecisions[idx].responsible = val.trim();
  }
}

function addNewPreviewItem() {
  if (!currentPreviewAta) return;
  const baseDate = new Date(currentPreviewAta.date || new Date());
  const dueDate = new Date(baseDate.getTime() + 15 * 86400000).toISOString().split('T')[0];

  currentPreviewDecisions.push({
    id: 'dec_' + Date.now() + '_' + currentPreviewDecisions.length,
    ataId: currentPreviewAta.id,
    ataTitle: currentPreviewAta.title,
    action: 'Nova atribuição ou decisão',
    responsible: 'Responsável',
    dueDate,
    decisionType: 'FOLLOW_UP_ASSIGNMENT',
    typeLabel: 'Atribuição de acompanhamento',
    days: 15,
    completed: false
  });

  renderPreviewTable();
}

function removePreviewItem(idx) {
  currentPreviewDecisions.splice(idx, 1);
  renderPreviewTable();
}

function cancelPreview() {
  const previewSection = document.getElementById('previewSection');
  if (previewSection) {
    previewSection.classList.add('hidden');
  }
  currentPreviewDecisions = [];
  currentPreviewAta = null;
}

function confirmSaveAta() {
  if (!currentPreviewAta || currentPreviewDecisions.length === 0) {
    alert('Nenhuma deliberação para salvar.');
    return;
  }

  atas.unshift(currentPreviewAta);
  decisions.unshift(...currentPreviewDecisions);

  saveState();

  // Resetar formulário
  document.getElementById('ataForm').reset();
  const todayStr = new Date().toISOString().split('T')[0];
  const dateInput = document.getElementById('ataDate');
  if (dateInput) dateInput.value = todayStr;
  
  const statusEl = document.getElementById('fileUploadStatus');
  if (statusEl) statusEl.classList.add('hidden');

  cancelPreview();

  updateDashboard();
  renderTasks();
  renderAtas();
  switchTab('alerts');

  alert(`Ata "${currentPreviewAta.title}" e suas ${currentPreviewDecisions.length} deliberações foram cadastradas com sucesso!`);
}

// Interpretador aprimorado de deliberações e prazos em 3 tipos:
// 1. "Atribuição de acompanhamento" (sem prazo explícito -> 15 dias padrão)
// 2. "Decisão permanente de procedimento" (regras, sem prazo)
// 3. "Ação com prazo" (prazo explícito em dias ou data)
function extractDecisionsFromText(text, ataTitle, ataId, baseDateStr) {
  if (!text || typeof text !== 'string') return [];

  // Normalizar quebras de linha e tentar segmentar por itens numerados se estiver tudo em texto corrido
  let rawText = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  
  // Se contiver números como " 1 - " ou " 1. " ou " 2) " no meio de frases sem quebra de linha, inserir quebra
  rawText = rawText.replace(/(\s+)(?=\d+[\.\-\)]\s+[A-ZÀ-Ú])/g, '\n');
  rawText = rawText.replace(/(\s+)(?=[•\-\*]\s+[A-ZÀ-Ú])/g, '\n');

  // Quebrar por linhas
  const rawLines = rawText.split('\n').map(l => l.trim()).filter(l => l.length > 2);
  
  // Agrupar linhas que pertençam a tópicos ou itens de ata
  const candidateItems = [];
  let currentBlock = "";

  rawLines.forEach(line => {
    const isHeaderOnly = /^(ata\s+da\s+reunião|reunião\s+ordinária|local:|horário:|data:|presentes:|participantes:|ordem\s+do\s+dia:?)$/i.test(line);
    if (isHeaderOnly) return;

    const isNewItem = /^(\d+[\.\-\)]|[a-zA-Z][\.\)]|•|\-|\*|Item\s+\d+|Assunto\s+\d+|Pauta\s+\d+)/i.test(line);

    if (isNewItem) {
      if (currentBlock.trim().length > 5) {
        candidateItems.push(currentBlock.trim());
      }
      currentBlock = line;
    } else {
      if (currentBlock.length > 0) {
        currentBlock += " " + line;
      } else {
        currentBlock = line;
      }
    }
  });

  if (currentBlock.trim().length > 5) {
    candidateItems.push(currentBlock.trim());
  }

  // Se não encontrou blocos por marcadores, usar as próprias linhas com mais de 10 caracteres
  const finalItems = candidateItems.length > 0 ? candidateItems : rawLines.filter(l => l.length > 10);

  const results = [];
  const baseDate = new Date(baseDateStr || new Date());

  finalItems.forEach((itemText, index) => {
    // Ignorar linhas puramente de cabeçalho
    if (/^(ata\s+da\s+reunião|pauta:|participantes:|presentes:)/i.test(itemText) && itemText.length < 50) {
      return;
    }

    let responsible = "Comissão / Designados";
    let action = itemText;
    let daysToAdd = 15; // PADRÃO DE 15 DIAS PARA ATRIBUIÇÕES SEM PRAZO EXPLÍCITO
    let decisionType = "FOLLOW_UP_ASSIGNMENT";
    let typeLabel = "Atribuição de acompanhamento";

    // 1. Verificar se é Decisão permanente de procedimento
    const isPermanent = itemText.toLowerCase().includes('sempre') ||
                        itemText.toLowerCase().includes('regra permanente') ||
                        itemText.toLowerCase().includes('procedimento') ||
                        itemText.toLowerCase().includes('diretriz') ||
                        itemText.toLowerCase().includes('permanente') ||
                        itemText.toLowerCase().includes('passa a valer') ||
                        itemText.toLowerCase().includes('relembrou ao corpo') ||
                        itemText.toLowerCase().includes('fica definido como padrão');

    if (isPermanent) {
      decisionType = "PERMANENT_PROCEDURE";
      typeLabel = "Decisão permanente de procedimento";
      daysToAdd = 3650; // Não expira / Regra permanente
    }

    // 2. Extrair Responsáveis
    // Formato 1: Nome entre parênteses no final ou meio: "(Edvaldo e Reginaldo)" ou "(Paulo, Milton)"
    const parenMatch = itemText.match(/\((([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?)(?:\s*(?:,|e|&)\s*([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?))*)\)/);
    if (parenMatch && parenMatch[1] && !parenMatch[1].toLowerCase().includes('dias')) {
      responsible = parenMatch[1].replace(/\s+e\s+/g, ', ').trim();
    } else {
      // Formato 2: "Responsável: Fulano" ou "Designados: Fulano e Beltrano"
      const prefixMatch = itemText.match(/(?:responsáveis?|designados?|encarregados?|atribuído\s+a):\s*([A-ZÀ-Ú][a-zà-ú\s,e]+)/i);
      if (prefixMatch) {
        responsible = prefixMatch[1].replace(/\s+e\s+/g, ', ').trim();
      } else {
        // Formato 3: "Fulano e Beltrano foram designados / ficaram responsáveis / vão falar..."
        const verbMatch = itemText.match(/([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?(?:\s+(?:e|,)\s+[A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?)*)\s+(?:foram designados|foram designadas|ficou responsável|ficaram responsáveis|vai falar|vão falar|irá falar|irão falar|conversará|conversarão|vai cuidar|vão cuidar|irá apresentar|apresentará|entregará)/i);
        if (verbMatch) {
          responsible = verbMatch[1].replace(/\s+e\s+/g, ', ').trim();
        } else {
          // Formato 4: Nomes conhecidos citados
          const knownNames = [
            "Edvaldo e Reginaldo", "Edvaldo, Reginaldo",
            "Paulo e Milton", "Paulo, Milton", "Paulo Freitas, José Milton",
            "Leandro e Leonardo", "Leandro, Leonardo",
            "Carlos e Mariana", "Carlos, Mariana", "Mariana Costa", "Roberto Santos", "Roberto"
          ];
          for (const k of knownNames) {
            if (itemText.includes(k)) {
              responsible = k.replace(/\s+e\s+/g, ', ');
              break;
            }
          }
        }
      }
    }

    // 3. Verificar Prazos Explícitos
    if (!isPermanent) {
      const daysMatch = itemText.match(/em\s+(\d+)\s+dias?/i) || itemText.match(/prazo\s+(?:de\s+)?(\d+)\s+dias?/i);
      if (daysMatch) {
        daysToAdd = parseInt(daysMatch[1], 10);
        decisionType = "ACTION_DEADLINE";
        typeLabel = `Ação com prazo (${daysToAdd}d)`;
      } else if (itemText.toLowerCase().includes('urgente') || itemText.toLowerCase().includes('imediato') || itemText.toLowerCase().includes('hoje')) {
        daysToAdd = 0;
        decisionType = "ACTION_DEADLINE";
        typeLabel = "Ação urgente (Hoje)";
      } else if (itemText.toLowerCase().includes('amanhã')) {
        daysToAdd = 1;
        decisionType = "ACTION_DEADLINE";
        typeLabel = "Ação com prazo (Amanhã)";
      } else if (itemText.toLowerCase().includes('7 dias') || itemText.toLowerCase().includes('uma semana')) {
        daysToAdd = 7;
        decisionType = "ACTION_DEADLINE";
        typeLabel = "Ação com prazo (7d)";
      } else if (itemText.toLowerCase().includes('30 dias') || itemText.toLowerCase().includes('um mês') || itemText.toLowerCase().includes('1 mês')) {
        daysToAdd = 30;
        decisionType = "ACTION_DEADLINE";
        typeLabel = "Ação com prazo (30d)";
      } else {
        // Sem prazo explícito -> ATRIBUIÇÃO DE ACOMPANHAMENTO: PADRÃO 15 DIAS
        daysToAdd = 15;
        decisionType = "FOLLOW_UP_ASSIGNMENT";
        typeLabel = "Atribuição de acompanhamento (15d padrão)";
      }
    }

    // Limpar prefixos numéricos do texto da ação (Ex: "1 - ", "2. ", "• ")
    let cleanAction = action
      .replace(/^(\d+[\.\-\)]|[a-zA-Z][\.\)]|•|\-|\*|Item\s+\d+:?|Assunto\s+\d+:?|Pauta\s+\d+:?)\s*/i, '')
      .replace(/\s+/g, ' ')
      .trim();

    if (cleanAction.length < 5) cleanAction = action.trim();

    const dueDate = new Date(baseDate.getTime() + daysToAdd * 86400000).toISOString().split('T')[0];

    results.push({
      id: 'dec_' + Date.now() + '_' + index,
      ataId,
      ataTitle,
      action: cleanAction,
      responsible,
      dueDate,
      decisionType,
      typeLabel,
      days: daysToAdd,
      completed: false
    });
  });

  return results;
}

function toggleTaskComplete(id) {
  const task = decisions.find(d => d.id === id);
  if (task) {
    task.completed = !task.completed;
    saveState();
    updateDashboard();
    renderTasks();
    renderAtas();
  }
}

function deleteDecision(id) {
  if (confirm('Deseja realmente remover esta deliberação?')) {
    decisions = decisions.filter(d => d.id !== id);
    saveState();
    updateDashboard();
    renderTasks();
    renderAtas();
  }
}

function deleteAta(id) {
  if (confirm('Excluir esta ata removerá todas as deliberações vinculadas a ela. Continuar?')) {
    atas = atas.filter(a => a.id !== id);
    decisions = decisions.filter(d => d.ataId !== id);
    saveState();
    updateDashboard();
    renderTasks();
    renderAtas();
  }
}

function saveState() {
  localStorage.setItem('gestor_atas_list', JSON.stringify(atas));
  localStorage.setItem('gestor_decisions_list', JSON.stringify(decisions));
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return dateStr;
}

function escapeHtml(text) {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function loadSampleAta() {
  const titleInput = document.getElementById('ataTitle');
  const contentInput = document.getElementById('ataContent');
  const dateInput = document.getElementById('ataDate');

  if (titleInput) titleInput.value = "Ata de Reunião - 26 de Agosto de 2026";
  if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
  if (contentInput) {
    contentInput.value = 
`1 - Falar com o pai e o irmão de Ítalo sobre a leitura. Edvaldo e Reginaldo foram designados.
2 - Consultar testemunhas e verificar contexto com os envolvidos (Paulo e Milton).
3 - Leandro e Leonardo irão falar com Eduardo sobre o assunto em 7 dias.
4 - Sempre deixar que os designados cuidem dos assuntos envolvendo os publicadores (Regra Permanente).`;
  }

  autoTriggerInterpretation();
}

function seedInitialData() {
  const today = new Date();
  const yesterday = new Date(today.getTime() - 86400000).toISOString().split('T')[0];
  const in15Days = new Date(today.getTime() + 15 * 86400000).toISOString().split('T')[0];
  const in7Days = new Date(today.getTime() + 7 * 86400000).toISOString().split('T')[0];
  const in10Years = new Date(today.getTime() + 3650 * 86400000).toISOString().split('T')[0];

  atas = [
    {
      id: 'ata_demo_1',
      title: 'Ata de Reunião - 26 de Agosto de 2026',
      date: yesterday,
      location: 'Sala do Corpo',
      content: 'Deliberações sobre atribuições de acompanhamento e procedimentos permanentes.',
      createdAt: new Date().toISOString()
    }
  ];

  decisions = [
    {
      id: 'dec_1',
      ataId: 'ata_demo_1',
      ataTitle: 'Ata de Reunião - 26 de Agosto de 2026',
      action: 'Falar com o pai e o irmão de Ítalo sobre a leitura',
      responsible: 'Edvaldo, Reginaldo',
      dueDate: in15Days, // 15 dias de prazo padrão para atribuição de acompanhamento
      decisionType: 'FOLLOW_UP_ASSIGNMENT',
      typeLabel: 'Atribuição de acompanhamento',
      completed: false
    },
    {
      id: 'dec_2',
      ataId: 'ata_demo_1',
      ataTitle: 'Ata de Reunião - 26 de Agosto de 2026',
      action: 'Consultar testemunhas e verificar contexto com os envolvidos',
      responsible: 'Paulo Freitas, José Milton',
      dueDate: in15Days,
      decisionType: 'FOLLOW_UP_ASSIGNMENT',
      typeLabel: 'Atribuição de acompanhamento',
      completed: false
    },
    {
      id: 'dec_3',
      ataId: 'ata_demo_1',
      ataTitle: 'Ata de Reunião - 26 de Agosto de 2026',
      action: 'Falar com Eduardo sobre o assunto',
      responsible: 'Leandro Silva, Leonardo dos Santos',
      dueDate: in7Days,
      decisionType: 'ACTION_DEADLINE',
      typeLabel: 'Ação com prazo',
      completed: false
    },
    {
      id: 'dec_4',
      ataId: 'ata_demo_1',
      ataTitle: 'Ata de Reunião - 26 de Agosto de 2026',
      action: 'Sempre deixar que os designados cuidem dos assuntos envolvendo os publicadores',
      responsible: 'Todos os Anciãos / Corpo',
      dueDate: in10Years,
      decisionType: 'PERMANENT_PROCEDURE',
      typeLabel: 'Decisão permanente de procedimento',
      completed: false
    }
  ];

  saveState();
}
