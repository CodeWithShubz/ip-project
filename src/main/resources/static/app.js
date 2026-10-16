/**
 * TravelAgg – Frontend Application Logic (Vanilla JavaScript)
 * Communicates with the Spring Boot REST API at /api/*
 */

// ─── Constants ────────────────────────────────────────────────────────────────
const API_BASE = '/api';
const AUTOCOMPLETE_DEBOUNCE_MS = 300;
const AUTOCOMPLETE_MIN_CHARS  = 2;

// ─── State ────────────────────────────────────────────────────────────────────
let allResults     = [];   // Raw results from last search
let filteredResults = [];  // After sidebar filters applied
let currentJourney = null; // Journey selected for booking
let selectedMode   = '';   // '' | 'FLIGHT' | 'TRAIN' | 'BUS'
let fareCalcTimer  = null; // Debounce handle for fare calculation

// Autocomplete state
let acDebounceTimers = {};
let acActiveIndex    = { 'origin-input': -1, 'dest-input': -1 };
let acSuggestions    = { 'origin-input': [], 'dest-input': [] };

// ─── DOM Ready ────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    // Set default travel date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dd   = String(tomorrow.getDate()).padStart(2, '0');
    const mm   = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const yyyy = tomorrow.getFullYear();
    // Text field shows DD/MM/YYYY
    document.getElementById('travel-date').value = `${dd}/${mm}/${yyyy}`;
    // Hidden native picker: set minimum to today, default to tomorrow
    const todayIso    = new Date().toISOString().split('T')[0]; // yyyy-MM-dd
    const tomorrowIso = `${yyyy}-${mm}-${dd}`;
    const picker = document.getElementById('travel-date-picker');
    picker.min   = todayIso;
    picker.value = tomorrowIso;

    // Attach autocomplete handlers
    setupAutocomplete('origin-input', 'origin-suggestions', '/api/journeys/origins');
    setupAutocomplete('dest-input',   'dest-suggestions',   '/api/journeys/destinations');

    // Prevent identical origin and destination on blur/change
    const validateDistinctCities = () => {
        const o = document.getElementById('origin-input').value.trim();
        const d = document.getElementById('dest-input').value.trim();
        if (o && d && o.toLowerCase() === d.toLowerCase()) {
            showToast('Origin and destination cannot be the same', 'error');
        }
    };
    document.getElementById('origin-input').addEventListener('change', validateDistinctCities);
    document.getElementById('dest-input').addEventListener('change', validateDistinctCities);

    // Close suggestions on outside click
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.autocomplete-wrapper') && !e.target.closest('.date-picker-wrapper')) {
            closeAllDropdowns();
        }
    });
});

// ─── Date Picker Helpers ──────────────────────────────────────────────────────

/**
 * Opens the native calendar by programmatically showing the hidden date input.
 * Falls back gracefully in browsers that don't support showPicker().
 */
function openCalendar() {
    const picker = document.getElementById('travel-date-picker');
    try {
        picker.showPicker();
    } catch (e) {
        // Fallback: make it momentarily visible and click it
        picker.style.pointerEvents = 'auto';
        picker.style.opacity = '0.01';
        picker.click();
        setTimeout(() => {
            picker.style.pointerEvents = 'none';
            picker.style.opacity = '0';
        }, 200);
    }
}

/**
 * Called when the hidden native date picker value changes.
 * Converts yyyy-MM-dd → DD/MM/YYYY and writes to the visible text input.
 */
function onNativeDateChange(picker) {
    if (!picker.value) return;
    const [y, m, d] = picker.value.split('-');
    document.getElementById('travel-date').value = `${d}/${m}/${y}`;
    setDateError('');
}

/**
 * Auto-inserts slashes while the user types so the field guides them
 * toward DD/MM/YYYY without being obtrusive.
 */
function onDateTextInput(input) {
    let v = input.value.replace(/[^\d/]/g, ''); // allow digits and slashes only
    // Auto-insert slashes
    if (v.length === 2 && !v.includes('/')) v += '/';
    else if (v.length === 5 && v.split('/').length === 2) v += '/';
    input.value = v;
    setDateError(''); // clear on each keystroke
}

/**
 * On blur, validate the typed date and sync the hidden picker.
 * Shows an inline error for invalid formats or impossible dates.
 */
function onDateTextBlur(input) {
    const val = input.value.trim();
    if (!val) return; // empty is validated at submit time
    const iso = parseDdMmYyyy(val);
    if (!iso) {
        setDateError('Invalid date – use DD/MM/YYYY, e.g. 17/10/2026');
        return;
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const selected = new Date(iso);
    if (selected <= today) {
        setDateError('Date must be in the future');
        return;
    }
    // Sync the hidden native picker
    const picker = document.getElementById('travel-date-picker');
    picker.value = iso;
    setDateError('');
}

/** Show or clear the inline date error message. */
function setDateError(msg) {
    const el = document.getElementById('date-error-msg');
    if (el) el.textContent = msg;
    const input = document.getElementById('travel-date');
    if (input) input.classList.toggle('error', !!msg);
}

/**
 * Parse a DD/MM/YYYY string and return 'yyyy-MM-dd' (ISO) or null if invalid.
 * Catches impossible dates like 31/02/2026.
 */
function parseDdMmYyyy(str) {
    if (!str) return null;
    const parts = str.split('/');
    if (parts.length !== 3) return null;
    const [dd, mm, yyyy] = parts;
    if (!dd || !mm || !yyyy || dd.length > 2 || mm.length > 2 || yyyy.length !== 4) return null;
    const d = parseInt(dd, 10);
    const m = parseInt(mm, 10);
    const y = parseInt(yyyy, 10);
    if (isNaN(d) || isNaN(m) || isNaN(y)) return null;
    if (m < 1 || m > 12 || d < 1 || d > 31) return null;
    // Use Date to catch impossible combos (e.g. 31/02/2026)
    const dt = new Date(y, m - 1, d);
    if (dt.getFullYear() !== y || dt.getMonth() + 1 !== m || dt.getDate() !== d) return null;
    return `${yyyy}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
}

/**
 * Shared utility: reads the travel-date text field, validates it,
 * and returns the ISO string for backend calls, or null on failure.
 * Shows toast / inline error automatically.
 */
function getValidatedIsoDate() {
    const raw = document.getElementById('travel-date').value.trim();
    if (!raw) {
        showToast('Please enter a travel date', 'error');
        setDateError('Please enter a travel date');
        return null;
    }
    const iso = parseDdMmYyyy(raw);
    if (!iso) {
        showToast('Invalid date – use DD/MM/YYYY, e.g. 17/10/2026', 'error');
        setDateError('Invalid date – use DD/MM/YYYY, e.g. 17/10/2026');
        return null;
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (new Date(iso) <= today) {
        showToast('Travel date must be in the future', 'error');
        setDateError('Date must be in the future');
        return null;
    }
    setDateError('');
    return iso; // 'yyyy-MM-dd'
}

// ─── City Autocomplete ─────────────────────────────────────────────────────────

function setupAutocomplete(inputId, suggestionsId, endpoint) {
    const input    = document.getElementById(inputId);
    const dropdown = document.getElementById(suggestionsId);

    input.addEventListener('input', () => {
        const val = input.value.trim();
        // Clear immediately if too short
        if (val.length < AUTOCOMPLETE_MIN_CHARS) {
            dropdown.innerHTML = '';
            acSuggestions[inputId] = [];
            acActiveIndex[inputId] = -1;
            return;
        }

        // Debounce
        clearTimeout(acDebounceTimers[inputId]);
        acDebounceTimers[inputId] = setTimeout(() => {
            fetchSuggestions(inputId, suggestionsId, endpoint, val);
        }, AUTOCOMPLETE_DEBOUNCE_MS);
    });

    // Keyboard navigation
    input.addEventListener('keydown', (e) => {
        const items = acSuggestions[inputId] || [];
        if (items.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            acActiveIndex[inputId] = Math.min(acActiveIndex[inputId] + 1, items.length - 1);
            highlightItem(suggestionsId, acActiveIndex[inputId]);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            acActiveIndex[inputId] = Math.max(acActiveIndex[inputId] - 1, 0);
            highlightItem(suggestionsId, acActiveIndex[inputId]);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (acActiveIndex[inputId] >= 0 && acActiveIndex[inputId] < items.length) {
                selectSuggestion(inputId, suggestionsId, items[acActiveIndex[inputId]]);
            } else if (items.length > 0) {
                selectSuggestion(inputId, suggestionsId, items[0]);
            }
        } else if (e.key === 'Escape') {
            dropdown.innerHTML = '';
            acSuggestions[inputId] = [];
            acActiveIndex[inputId] = -1;
        }
    });
}

async function fetchSuggestions(inputId, suggestionsId, endpoint, query) {
    const dropdown = document.getElementById(suggestionsId);

    // Show loading indicator
    dropdown.innerHTML = '<div class="suggestion-item suggestion-loading">Searching…</div>';

    try {
        const res = await fetch(`${endpoint}?q=${encodeURIComponent(query)}`);
        if (!res.ok) {
            dropdown.innerHTML = '<div class="suggestion-item suggestion-empty">Error loading suggestions</div>';
            return;
        }
        const json = await res.json();
        const data = json.data || [];

        // Deduplicate by city name and location details
        const seen = new Set();
        const unique = [];
        for (const item of data) {
            const city = typeof item === 'string' ? item : (item.city || '');
            const display = typeof item === 'string' ? item : (item.display || city);
            const locationType = typeof item === 'object' ? (item.locationType || '') : '';
            const stationName  = typeof item === 'object' ? (item.stationName  || '') : '';
            const stationCode  = typeof item === 'object' ? (item.stationCode  || '') : '';
            const key = display.toLowerCase();
            if (!seen.has(key) && city) {
                seen.add(key);
                unique.push({ city, display, locationType, stationName, stationCode });
            }
            if (unique.length >= 10) break;
        }

        acSuggestions[inputId] = unique;
        acActiveIndex[inputId] = -1;

        if (unique.length === 0) {
            dropdown.innerHTML = '<div class="suggestion-item suggestion-empty">No matching cities found</div>';
            return;
        }

        dropdown.innerHTML = unique.map((item, idx) => {
            const typeIcon = getTypeIcon(item);
            return `<div class="suggestion-item" data-index="${idx}"
                         onmousedown="selectSuggestion('${inputId}', '${suggestionsId}', acSuggestions['${inputId}'][${idx}])"
                         onmouseenter="highlightItem('${suggestionsId}', ${idx})">${typeIcon}${escapeHtml(item.display)}</div>`;
        }).join('');

    } catch (e) {
        console.error('Autocomplete error:', e);
        dropdown.innerHTML = '<div class="suggestion-item suggestion-empty">Failed to load suggestions</div>';
    }
}

function getTypeIcon(item) {
    // For simple string items
    if (!item.locationType && !item.stationName) return '';
    // For rich location objects
    const type = (item.locationType || '').toUpperCase();
    if (type.includes('AIRPORT'))  return '<span class="suggestion-type-icon">✈️</span>';
    if (type.includes('RAILWAY'))  return '<span class="suggestion-type-icon">🚆</span>';
    if (type.includes('BUS'))      return '<span class="suggestion-type-icon">🚌</span>';
    return '';
}

function selectSuggestion(inputId, suggestionsId, item) {
    const input = document.getElementById(inputId);
    // Always use the city name for the actual value (the backend searches by city)
    input.value = item.city || item.display || item;
    document.getElementById(suggestionsId).innerHTML = '';
    acSuggestions[inputId] = [];
    acActiveIndex[inputId] = -1;

    // Validate: if both origin and destination are filled, check they aren't the same
    const origin = document.getElementById('origin-input').value.trim();
    const dest   = document.getElementById('dest-input').value.trim();
    if (origin && dest && origin.toLowerCase() === dest.toLowerCase()) {
        showToast('Origin and destination cannot be the same', 'error');
    }
}

function highlightItem(suggestionsId, index) {
    const dropdown = document.getElementById(suggestionsId);
    const items = dropdown.querySelectorAll('.suggestion-item:not(.suggestion-loading):not(.suggestion-empty)');
    items.forEach((el, i) => {
        el.classList.toggle('suggestion-active', i === index);
    });
    // Scroll into view
    if (items[index]) {
        items[index].scrollIntoView({ block: 'nearest' });
    }
}

function closeAllDropdowns() {
    document.querySelectorAll('.suggestions').forEach(el => el.innerHTML = '');
    Object.keys(acSuggestions).forEach(k => {
        acSuggestions[k] = [];
        acActiveIndex[k] = -1;
    });
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ─── Mode Tabs ────────────────────────────────────────────────────────────────
function setMode(btn) {
    document.querySelectorAll('.mode-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    selectedMode = btn.dataset.mode;
}

function swapCities() {
    closeAllDropdowns();
    const orig = document.getElementById('origin-input');
    const dest = document.getElementById('dest-input');
    [orig.value, dest.value] = [dest.value, orig.value];

    // Animate swap button
    const btn = document.getElementById('swap-btn');
    if (btn) {
        btn.style.transform = 'rotate(180deg)';
        setTimeout(() => btn.style.transform = '', 300);
    }
}

/**
 * Fill origin and destination from a popular-route chip click.
 * Scrolls the search card into view and focuses the date field.
 */
function fillRoute(origin, destination) {
    document.getElementById('origin-input').value = origin;
    document.getElementById('dest-input').value   = destination;
    closeAllDropdowns();
    document.getElementById('search-card').scrollIntoView({ behavior: 'smooth', block: 'center' });
    setTimeout(() => document.getElementById('travel-date').focus(), 400);
}

// ─── Search ──────────────────────────────────────────────────────────────────
async function searchJourneys() {
    // Validate date first using shared helper (returns ISO string or null)
    const travelDate = getValidatedIsoDate();
    if (!travelDate) return;

    const origin      = document.getElementById('origin-input').value.trim();
    const destination = document.getElementById('dest-input').value.trim();
    const passengers  = 1;

    // Client-side validation
    if (!origin)      { showToast('Please enter an origin city', 'error'); return; }
    if (!destination) { showToast('Please enter a destination city', 'error'); return; }
    if (origin.toLowerCase() === destination.toLowerCase()) {
        showToast('Origin and destination cannot be the same', 'error'); return;
    }

    setSearchLoading(true);
    document.getElementById('results-section').classList.add('hidden');

    const payload = {
        origin, destination,
        travelDate, // ISO yyyy-MM-dd as expected by Spring Boot LocalDate
        passengers,
        transportMode: selectedMode || null,
        sortBy: 'PRICE'
    };

    try {
        const res  = await fetch(`${API_BASE}/journeys/search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();

        if (!res.ok) {
            showToast(json.message || 'Search failed', 'error');
            return;
        }

        allResults      = json.data || [];
        filteredResults = [...allResults];

        renderResults(origin, destination, passengers);

        document.getElementById('results-section').classList.remove('hidden');
        document.getElementById('results-section').scrollIntoView({ behavior: 'smooth', block: 'start' });

        if (allResults.length === 0) {
            showToast(json.message || 'No journeys found for this route.', 'info');
        }

    } catch (err) {
        showToast('Unable to reach the server. Is Spring Boot running?', 'error');
        console.error(err);
    } finally {
        setSearchLoading(false);
    }
}

function setSearchLoading(loading) {
    const btn  = document.getElementById('search-btn');
    const text = document.getElementById('search-btn-text');
    const spin = document.getElementById('search-spinner');
    btn.disabled = loading;
    text.textContent = loading ? 'Searching…' : 'Search';
    spin.classList.toggle('hidden', !loading);

    // Show skeleton cards while searching
    if (loading) {
        const list = document.getElementById('journey-list');
        const none = document.getElementById('no-results');
        if (list) {
            list.innerHTML = Array.from({ length: 3 }).map(() => `
            <div class="journey-card" style="opacity:1;cursor:default;pointer-events:none">
                <div class="skeleton" style="width:48px;height:48px;border-radius:12px;flex-shrink:0"></div>
                <div class="journey-info" style="flex:1">
                    <div class="skeleton" style="height:12px;width:60%;margin-bottom:10px"></div>
                    <div class="skeleton" style="height:28px;width:80%"></div>
                    <div class="skeleton" style="height:10px;width:40%;margin-top:10px"></div>
                </div>
                <div style="text-align:right;min-width:110px">
                    <div class="skeleton" style="height:28px;width:90px;margin-left:auto;margin-bottom:8px"></div>
                    <div class="skeleton" style="height:34px;width:80px;margin-left:auto"></div>
                </div>
            </div>`).join('');
        }
        if (none) none.classList.add('hidden');
        document.getElementById('results-section').classList.remove('hidden');
    }
}

// ─── Results Rendering ────────────────────────────────────────────────────────
function renderResults(origin, destination, passengers) {
    const list  = document.getElementById('journey-list');
    const count = document.getElementById('results-count');
    const route = document.getElementById('results-route');
    const none  = document.getElementById('no-results');

    // Update price slider max based on results
    if (filteredResults.length > 0) {
        const maxFare = Math.max(...filteredResults.map(j => j.baseFare));
        const slider  = document.getElementById('price-range-slider');
        slider.max   = Math.ceil(maxFare / 1000) * 1000;
        slider.value = slider.max;
        document.getElementById('price-max-label').textContent = `₹${Number(slider.max).toLocaleString('en-IN')}`;
        document.getElementById('price-display').textContent   = `₹${Number(slider.max).toLocaleString('en-IN')}`;
    }

    count.textContent = `${filteredResults.length} Result${filteredResults.length !== 1 ? 's' : ''}`;
    route.textContent = origin && destination ? `${origin} → ${destination}` : '';

    if (filteredResults.length === 0) {
        list.innerHTML = '';
        none.classList.remove('hidden');
        return;
    }
    none.classList.add('hidden');

    list.innerHTML = filteredResults.map((j, i) => buildJourneyCard(j, passengers, i)).join('');
}

function buildJourneyCard(j, passengers, idx) {
    const modeIcons = { FLIGHT: '✈️', TRAIN: '🚆', BUS: '🚌' };
    const icon      = modeIcons[j.transportMode] || '🚗';
    const dep       = formatTime(j.departureTime);
    const arr       = formatTime(j.arrivalTime);
    const depCity   = j.origin.substring(0, 3).toUpperCase();
    const arrCity   = j.destination.substring(0, 3).toUpperCase();
    const amenities = (j.amenities || []).slice(0, 4).map(a => `<span class="amenity-tag">${a.trim()}</span>`).join('');
    const seats     = j.availableSeats;
    const seatsClass = seats <= 10 ? 'low' : '';
    const seatsText  = seats <= 5  ? `⚠️ Only ${seats} left` : `${seats} seats`;
    const stars     = '⭐'.repeat(Math.round(j.rating || 4));
    const effFare   = j.effectiveFarePerSeat || j.baseFare;

    return `
    <div class="journey-card" id="journey-card-${j.id}" style="animation-delay:${idx * 0.06}s">
        <div class="mode-badge ${j.transportMode}">${icon}</div>
        <div class="journey-info">
            <div class="journey-operator">${j.operator} · ${j.transportMode}</div>
            <div class="journey-times">
                <div class="time-block">
                    <div class="time">${dep}</div>
                    <div class="city">${depCity}</div>
                </div>
                <div class="duration-pill">
                    <div class="duration-line"></div>
                    <div class="duration-text">${j.durationFormatted}</div>
                </div>
                <div class="time-block">
                    <div class="time">${arr}</div>
                    <div class="city">${arrCity}</div>
                </div>
            </div>
            <div class="journey-amenities">${amenities}</div>
        </div>
        <div class="journey-price-block">
            <div class="price-label">Base fare / seat</div>
            <div class="price-main">₹${Number(j.baseFare).toLocaleString('en-IN')}</div>
            <div class="price-effective">Effective: ₹${Number(effFare).toFixed(0)}</div>
            <div class="rating">${stars} ${j.rating}</div>
            <div class="seats-badge ${seatsClass}">${seatsText}</div>
            <div class="demo-label">⚠️ Simulated fare</div>
        </div>
        <button class="book-btn" id="book-btn-${j.id}" onclick="openBookingModal(${j.id}, ${passengers})">
            Book Now
        </button>
    </div>`;
}

// ─── Filter & Sort (client-side for speed) ────────────────────────────────────
function applyFilters() {
    const showFlight = document.getElementById('filter-flight').checked;
    const showTrain  = document.getElementById('filter-train').checked;
    const showBus    = document.getElementById('filter-bus').checked;
    const maxPrice   = parseFloat(document.getElementById('price-range-slider').value);

    document.getElementById('price-display').textContent = `₹${Number(maxPrice).toLocaleString('en-IN')}`;

    filteredResults = allResults.filter(j => {
        const modeOk  = (j.transportMode === 'FLIGHT' && showFlight)
                     || (j.transportMode === 'TRAIN'  && showTrain)
                     || (j.transportMode === 'BUS'    && showBus);
        const priceOk = j.baseFare <= maxPrice;
        return modeOk && priceOk;
    });

    // Re-apply current sort
    const activeSortBtn = document.querySelector('.sort-btn.active');
    const sortKey = activeSortBtn?.id?.replace('sort-', '').toUpperCase() || 'PRICE';
    sortResults(sortKey, null, true);

    renderResults(
        document.getElementById('origin-input').value,
        document.getElementById('dest-input').value,
        1
    );
}

function sortResults(key, btn, skipRender = false) {
    if (btn) {
        document.querySelectorAll('.sort-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
    }
    switch (key) {
        case 'DURATION':  filteredResults.sort((a, b) => a.durationMinutes - b.durationMinutes); break;
        case 'DEPART':
        case 'DEPARTURE': filteredResults.sort((a, b) => new Date(a.departureTime) - new Date(b.departureTime)); break;
        default:          filteredResults.sort((a, b) => a.baseFare - b.baseFare);
    }
    if (!skipRender) {
        renderResults(
            document.getElementById('origin-input').value,
            document.getElementById('dest-input').value,
            parseInt(document.getElementById('passengers').value) || 1
        );
    }
}

// ─── Booking Modal ─────────────────────────────────────────────────────────────
async function openBookingModal(journeyId, passengers) {
    try {
        const res  = await fetch(`${API_BASE}/journeys/${journeyId}`);
        if (!res.ok) {
            const json = await res.json().catch(() => ({}));
            showToast(json.message || 'Failed to load journey', 'error');
            return;
        }
        const json = await res.json();
        currentJourney = json.data;
    } catch (e) {
        showToast('Cannot load journey details', 'error'); return;
    }

    // Populate journey summary
    const j   = currentJourney;
    const dep = formatTime(j.departureTime);
    const arr = formatTime(j.arrivalTime);
    document.getElementById('booking-journey-summary').innerHTML = `
        <div class="bjs-item"><span class="bjs-label">Mode</span><span class="bjs-value">${j.transportMode}</span></div>
        <div class="bjs-item"><span class="bjs-label">Operator</span><span class="bjs-value">${j.operator}</span></div>
        <div class="bjs-item"><span class="bjs-label">Route</span><span class="bjs-value">${j.origin} → ${j.destination}</span></div>
        <div class="bjs-item"><span class="bjs-label">Departure</span><span class="bjs-value">${dep}</span></div>
        <div class="bjs-item"><span class="bjs-label">Arrival</span><span class="bjs-value">${arr}</span></div>
        <div class="bjs-item"><span class="bjs-label">Duration</span><span class="bjs-value">${j.durationFormatted}</span></div>
        <!-- Demo note removed for professional appearance -->
    `;

    // Pre-fill passenger count
    document.getElementById('b-pax').value = passengers;

    // Clear form
    ['b-name','b-email','b-phone','promo-code'].forEach(id => document.getElementById(id).value = '');
    document.querySelectorAll('.addon-card input[type="checkbox"]').forEach(cb => cb.checked = false);
    clearErrors();

    // Show modal
    document.getElementById('booking-modal-overlay').classList.remove('hidden');
    document.body.style.overflow = 'hidden';

    // Initial fare calculation
    recalculateFare();
}

function closeBookingModal(event) {
    if (event && event.target !== document.getElementById('booking-modal-overlay')) return;
    document.getElementById('booking-modal-overlay').classList.add('hidden');
    document.body.style.overflow = '';
    currentJourney = null;
}

// ─── Fare Calculation ─────────────────────────────────────────────────────────
function recalculateFare() {
    clearTimeout(fareCalcTimer);
    fareCalcTimer = setTimeout(doCalculateFare, 400); // debounce 400ms
}

async function doCalculateFare() {
    if (!currentJourney) return;

    const passengers   = parseInt(document.getElementById('b-pax').value) || 1;
    const promoCode    = document.getElementById('promo-code').value.trim().toUpperCase();
    const selectedAddons = getSelectedAddons();

    document.getElementById('breakdown-loading').classList.remove('hidden');

    try {
        const res  = await fetch(`${API_BASE}/bookings/calculate-fare`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                journeyId: currentJourney.id,
                passengers, promoCode,
                selectedAddons
            })
        });
        if (!res.ok) {
            const json = await res.json().catch(() => ({}));
            showToast(json.message || 'Fare calculation failed', 'error');
            return;
        }
        const json = await res.json();

        const f = json.data;
        document.getElementById('bd-base').textContent    = `₹${fmt(f.baseFarePerSeat)}`;
        document.getElementById('bd-pax').textContent     = `× ${f.passengers} = ₹${fmt(f.baseFareTotal)}`;
        document.getElementById('bd-addons').textContent  = `₹${fmt(f.addonsCost)}`;
        document.getElementById('bd-service').textContent = `₹${fmt(f.serviceFee)}`;
        document.getElementById('bd-gst').textContent     = `₹${fmt(f.gstAmount)}`;
        const disc = parseFloat(f.discountAmount);
        document.getElementById('bd-discount').textContent = disc > 0 ? `−₹${fmt(f.discountAmount)}` : '–';
        document.getElementById('bd-total').textContent   = `₹${fmt(f.totalFare)}`;

        // Show promo feedback
        if (promoCode && disc === 0) showToast('Invalid promo code', 'error');
        else if (promoCode && disc > 0) showToast(`🎉 Promo applied! Saved ₹${fmt(f.discountAmount)}`, 'success');

    } catch (e) {
        console.error('Fare calculation failed', e);
        showToast('Fare calculation error. Please try again.', 'error');
    } finally {
        document.getElementById('breakdown-loading').classList.add('hidden');
    }
}

function getSelectedAddons() {
    return [...document.querySelectorAll('.addon-card input[type="checkbox"]:checked')]
        .map(cb => cb.value);
}

// ─── Booking Submission ────────────────────────────────────────────────────────
async function submitBooking(event) {
    event.preventDefault();
    if (!currentJourney) return;
    if (!validateBookingForm()) return;

    const confirmBtn  = document.getElementById('confirm-booking-btn');
    const confirmText = document.getElementById('confirm-text');
    const confirmSpin = document.getElementById('confirm-spinner');
    confirmBtn.disabled = true;
    confirmText.textContent = 'Processing…';
    confirmSpin.classList.remove('hidden');

    // Convert DD/MM/YYYY display value → ISO yyyy-MM-dd for Spring Boot LocalDate
    const travelDateIso = parseDdMmYyyy(document.getElementById('travel-date').value.trim());
    if (!travelDateIso) {
        showToast('Invalid travel date. Please select a valid date.', 'error');
        confirmBtn.disabled = false;
        confirmText.textContent = 'Confirm Booking';
        confirmSpin.classList.add('hidden');
        return;
    }

    const payload = {
        journeyId:      currentJourney.id,
        passengerName:  document.getElementById('b-name').value.trim(),
        passengerEmail: document.getElementById('b-email').value.trim(),
        passengerPhone: document.getElementById('b-phone').value.trim(),
        passengers:     parseInt(document.getElementById('b-pax').value),
        travelDate:     travelDateIso,  // ISO yyyy-MM-dd – fixes DateTimeParseException
        selectedAddons: getSelectedAddons(),
        promoCode:      document.getElementById('promo-code').value.trim().toUpperCase() || null
    };

    try {
        const res  = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();

        if (!res.ok) {
            const msg = json.message || 'Booking failed';
            // Show field-level validation errors if present
            if (json.data && typeof json.data === 'object') {
                Object.entries(json.data).forEach(([field, err]) => {
                    const el = document.getElementById(`err-${field.toLowerCase()}`);
                    if (el) el.textContent = err;
                });
            }
            showToast(msg, 'error');
            return;
        }

        const booking = json.data;
        closeBookingModal(null);
        showConfirmationModal(booking);

        // Refresh seat count in the result card
        const card = document.getElementById(`journey-card-${currentJourney.id}`);
        if (card) {
            const badge = card.querySelector('.seats-badge');
            if (badge) {
                const newSeats = currentJourney.availableSeats - payload.passengers;
                badge.textContent = newSeats <= 5 ? `⚠️ Only ${newSeats} left` : `${newSeats} seats`;
            }
        }

    } catch (e) {
        showToast('Network error. Please try again.', 'error');
        console.error(e);
    } finally {
        confirmBtn.disabled = false;
        confirmText.textContent = 'Confirm Booking';
        confirmSpin.classList.add('hidden');
    }
}

function validateBookingForm() {
    clearErrors();
    let valid = true;

    const name  = document.getElementById('b-name').value.trim();
    const email = document.getElementById('b-email').value.trim();
    const phone = document.getElementById('b-phone').value.trim();
    const pax   = parseInt(document.getElementById('b-pax').value);

    if (!name || name.length < 2) {
        setError('err-name', 'Name must be at least 2 characters'); valid = false;
    }
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setError('err-email', 'Enter a valid email address'); valid = false;
    }
    if (!phone || !/^[6-9]\d{9}$/.test(phone)) {
        setError('err-phone', 'Enter a valid 10-digit Indian mobile number'); valid = false;
    }
    if (!pax || pax < 1 || pax > 12) {
        setError('err-pax', 'Passengers must be between 1 and 12'); valid = false;
    }

    return valid;
}

function setError(id, msg) {
    const el = document.getElementById(id);
    if (el) el.textContent = msg;
    const inputId = id.replace('err-', 'b-');
    const inp = document.getElementById(inputId);
    if (inp) inp.classList.add('error');
}

function clearErrors() {
    ['err-name','err-email','err-phone','err-pax'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = '';
    });
    ['b-name','b-email','b-phone','b-pax'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.remove('error');
    });
}

// ─── Confirmation Modal ────────────────────────────────────────────────────────
let currentUserEmail = null;
function showConfirmationModal(booking) {
    currentUserEmail = booking.passengerEmail; // store for My Bookings auto-fetch
    document.getElementById('pnr-display').textContent = booking.pnrNumber;
    document.getElementById('confirm-details').innerHTML = `
        <strong>Route:</strong> ${booking.origin} → ${booking.destination}<br>
        <strong>Operator:</strong> ${booking.operator} (${booking.transportMode})<br>
        <strong>Departure:</strong> ${formatDateTime(booking.departureTime)}<br>
        <strong>Passengers:</strong> ${booking.passengers}<br>
        <strong>Name:</strong> ${booking.passengerName}<br>
        <strong>Email:</strong> ${booking.passengerEmail}<br>
        <strong>Total Paid:</strong> <span style="color:var(--clr-success);font-weight:700">₹${fmt(booking.totalFare)}</span>
        <!-- Demo warning removed -->
    `;
    document.getElementById('confirm-modal-overlay').classList.remove('hidden');
}

function closeConfirmModal() {
    document.getElementById('confirm-modal-overlay').classList.add('hidden');
    document.body.style.overflow = '';
}

// ─── My Bookings ──────────────────────────────────────────────────────────────
function showMyBookings() {
    closeConfirmModal();
    document.getElementById('my-bookings-list').innerHTML = '';
    const lookupInput = document.getElementById('pnr-lookup-input');
    // Prefill with stored email if available
    if (currentUserEmail) {
        lookupInput.value = currentUserEmail;
    } else {
        lookupInput.value = '';
    }
    document.getElementById('mybookings-modal-overlay').classList.remove('hidden');
    document.body.style.overflow = 'hidden';
    // Auto-fetch if we have an email
    if (currentUserEmail) {
        lookupBookings();
    }
}

function closeMyBookings(event) {
    if (event && event.target !== document.getElementById('mybookings-modal-overlay')) return;
    document.getElementById('mybookings-modal-overlay').classList.add('hidden');
    document.body.style.overflow = '';
}

async function lookupBookings() {
    const query = document.getElementById('pnr-lookup-input').value.trim();
    if (!query) { showToast('Enter a PNR or email', 'error'); return; }

    const listEl = document.getElementById('my-bookings-list');
    listEl.innerHTML = `<div style="display:flex;align-items:center;gap:12px;padding:24px 16px;color:var(--clr-text-muted);font-size:0.9rem">
        <div class="spinner" style="border-top-color:var(--clr-primary);border-color:var(--clr-border)"></div>
        Looking up bookings…
    </div>`;

    try {
        let res;
        if (query.includes('@')) {
            res = await fetch(`${API_BASE}/bookings/email/${encodeURIComponent(query)}`);
        } else {
            res = await fetch(`${API_BASE}/bookings/pnr/${query.toUpperCase()}`);
        }

        if (!res.ok) {
            const json = await res.json().catch(() => ({}));
            listEl.innerHTML = `<p style="color:var(--clr-danger);text-align:center;padding:20px">${json.message || 'Not found'}</p>`;
            return;
        }

        const json = await res.json();
        const bookings = Array.isArray(json.data) ? json.data : [json.data];
        listEl.innerHTML = bookings.map(b => buildBookingCard(b)).join('');

    } catch (e) {
        listEl.innerHTML = '<p style="color:var(--clr-danger);text-align:center;padding:20px">Error connecting to server</p>';
    }
}

function buildBookingCard(b) {
    const canCancel = b.bookingStatus === 'CONFIRMED';
    return `
    <div class="my-booking-card ${b.bookingStatus}" id="mbc-${b.bookingId}">
        <div class="mbc-header">
            <span class="mbc-pnr">PNR: ${b.pnrNumber}</span>
            <span class="mbc-status ${b.bookingStatus}">${b.bookingStatus}</span>
        </div>
        <div class="mbc-details">
            <strong>${b.origin} → ${b.destination}</strong> &nbsp;·&nbsp; ${b.transportMode} &nbsp;·&nbsp; ${b.operator}<br>
            Departure: ${formatDateTime(b.departureTime)}<br>
            Passengers: ${b.passengers} &nbsp;·&nbsp; Name: ${b.passengerName}<br>
            Total Paid: <strong style="color:var(--clr-success)">₹${fmt(b.totalFare)}</strong>
        </div>
        ${canCancel ? `<button class="mbc-cancel-btn" onclick="cancelBooking(${b.bookingId}, '${b.pnrNumber}')">Cancel Booking</button>` : ''}
    </div>`;
}

async function cancelBooking(bookingId, pnr) {
    if (!confirm(`Cancel booking PNR: ${pnr}? This cannot be undone.`)) return;

    try {
        const res  = await fetch(`${API_BASE}/bookings/${bookingId}/cancel`, { method: 'PUT' });
        if (!res.ok) {
            const json = await res.json().catch(() => ({}));
            showToast(json.message || 'Cancellation failed', 'error');
            return;
        }
        const json = await res.json();

        showToast(`Booking ${pnr} cancelled successfully.`, 'success');
        // Update card in place
        const card = document.getElementById(`mbc-${bookingId}`);
        if (card) {
            card.classList.replace('CONFIRMED', 'CANCELLED');
            card.querySelector('.mbc-status').className  = 'mbc-status CANCELLED';
            card.querySelector('.mbc-status').textContent = 'CANCELLED';
            const cancelBtn = card.querySelector('.mbc-cancel-btn');
            if (cancelBtn) cancelBtn.remove();
        }
    } catch (e) {
        showToast('Network error. Please try again.', 'error');
    }
}

// ─── Utilities ────────────────────────────────────────────────────────────────
function fmt(num) {
    return Number(num).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatTime(dt) {
    if (!dt) return '–';
    const d = new Date(dt);
    return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function formatDateTime(dt) {
    if (!dt) return '–';
    const d = new Date(dt);
    return d.toLocaleString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: false
    });
}

// ─── Toast Notification ───────────────────────────────────────────────────────
let toastTimer = null;
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const icons = { success: '✅ ', error: '❌ ', info: 'ℹ️ ' };
    toast.textContent = (icons[type] || '') + message;
    toast.className   = `toast ${type}`;
    toast.classList.remove('hidden');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.add('hidden'), 4500);
}
