/**
 * TravelAgg – Frontend Application Logic (Vanilla JavaScript)
 * Communicates with the Spring Boot REST API at /api/*
 */

// ─── Constants ────────────────────────────────────────────────────────────────
const API_BASE = '/api';

// ─── State ────────────────────────────────────────────────────────────────────
let allResults     = [];   // Raw results from last search
let filteredResults = [];  // After sidebar filters applied
let currentJourney = null; // Journey selected for booking
let selectedMode   = '';   // '' | 'FLIGHT' | 'TRAIN' | 'BUS
let fareCalcTimer  = null; // Debounce handle for fare calculation

// ─── DOM Ready ────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    // Set default travel date to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    document.getElementById('travel-date').value = tomorrow.toISOString().split('T')[0];
    document.getElementById('travel-date').min   = new Date().toISOString().split('T')[0];

    // Load city lists for autocomplete
    loadCities();

    // Close suggestions on outside click
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.autocomplete-wrapper')) {
            document.querySelectorAll('.suggestions').forEach(el => el.innerHTML = '');
        }
    });
});

// ─── City Autocomplete ─────────────────────────────────────────────────────────
let allCities = [];

async function loadCities() {
    try {
        const [origRes, destRes] = await Promise.all([
            fetch(`${API_BASE}/journeys/origins`),
            fetch(`${API_BASE}/journeys/destinations`)
        ]);
        const origData = await origRes.json();
        const destData = await destRes.json();

        const origins = origData.data || [];
        const dests   = destData.data || [];
        allCities = [...new Set([...origins, ...dests])].sort();
    } catch (e) {
        // Fallback demo cities if API not yet reachable
        allCities = ['Agra','Bangalore','Chennai','Delhi','Goa','Hyderabad','Kolkata','Mumbai','Pune'];
    }
}

function autocomplete(inputId, suggestionsId) {
    const input    = document.getElementById(inputId);
    const dropdown = document.getElementById(suggestionsId);
    const val      = input.value.trim().toLowerCase();

    if (val.length < 1) { dropdown.innerHTML = ''; return; }

    const matches = allCities.filter(c => c.toLowerCase().startsWith(val)).slice(0, 8);
    dropdown.innerHTML = matches.map(c =>
        `<div class="suggestion-item" onclick="selectCity('${inputId}', '${suggestionsId}', '${c}')">${c}</div>`
    ).join('');
}

function selectCity(inputId, suggestionsId, city) {
    document.getElementById(inputId).value = city;
    document.getElementById(suggestionsId).innerHTML = '';
}

// ─── Mode Tabs ────────────────────────────────────────────────────────────────
function setMode(btn) {
    document.querySelectorAll('.mode-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    selectedMode = btn.dataset.mode;
}

function swapCities() {
    const orig = document.getElementById('origin-input');
    const dest = document.getElementById('dest-input');
    [orig.value, dest.value] = [dest.value, orig.value];

    // Animate swap button
    const btn = document.getElementById('swap-btn');
    btn.style.transform = 'rotate(180deg)';
    setTimeout(() => btn.style.transform = '', 300);
}

// ─── Search ──────────────────────────────────────────────────────────────────
async function searchJourneys() {
    const origin      = document.getElementById('origin-input').value.trim();
    const destination = document.getElementById('dest-input').value.trim();
    const travelDate  = document.getElementById('travel-date').value;
    const passengers  = parseInt(document.getElementById('passengers').value) || 1;

    // Client-side validation
    if (!origin)      { showToast('Please enter an origin city', 'error'); return; }
    if (!destination) { showToast('Please enter a destination city', 'error'); return; }
    if (!travelDate)  { showToast('Please select a travel date', 'error'); return; }
    if (origin.toLowerCase() === destination.toLowerCase()) {
        showToast('Origin and destination cannot be the same', 'error'); return;
    }

    setSearchLoading(true);
    document.getElementById('results-section').classList.add('hidden');

    const payload = {
        origin, destination, travelDate, passengers,
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

        if (allResults.length === 0) showToast('No journeys found for this route.', 'info');

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
        parseInt(document.getElementById('passengers').value) || 1
    );
}

function sortResults(key, btn, skipRender = false) {
    if (btn) {
        document.querySelectorAll('.sort-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
    }
    switch (key) {
        case 'DURATION':  filteredResults.sort((a, b) => a.durationMinutes - b.durationMinutes); break;
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
        const json = await res.json();
        if (!res.ok) { showToast(json.message || 'Failed to load journey', 'error'); return; }
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
        const json = await res.json();
        if (!res.ok) return;

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

    const payload = {
        journeyId:      currentJourney.id,
        passengerName:  document.getElementById('b-name').value.trim(),
        passengerEmail: document.getElementById('b-email').value.trim(),
        passengerPhone: document.getElementById('b-phone').value.trim(),
        passengers:     parseInt(document.getElementById('b-pax').value),
        travelDate:     document.getElementById('travel-date').value,
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
function showConfirmationModal(booking) {
    document.getElementById('pnr-display').textContent = booking.pnrNumber;
    document.getElementById('confirm-details').innerHTML = `
        <strong>Route:</strong> ${booking.origin} → ${booking.destination}<br>
        <strong>Operator:</strong> ${booking.operator} (${booking.transportMode})<br>
        <strong>Departure:</strong> ${formatDateTime(booking.departureTime)}<br>
        <strong>Passengers:</strong> ${booking.passengers}<br>
        <strong>Name:</strong> ${booking.passengerName}<br>
        <strong>Email:</strong> ${booking.passengerEmail}<br>
        <strong>Total Paid:</strong> <span style="color:var(--clr-success);font-weight:700">₹${fmt(booking.totalFare)}</span>
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
    document.getElementById('pnr-lookup-input').value = '';
    document.getElementById('mybookings-modal-overlay').classList.remove('hidden');
    document.body.style.overflow = 'hidden';
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
    listEl.innerHTML = '<p style="color:var(--clr-text-muted);text-align:center;padding:20px">Loading…</p>';

    try {
        let res, json;
        if (query.includes('@')) {
            res  = await fetch(`${API_BASE}/bookings/email/${encodeURIComponent(query)}`);
            json = await res.json();
        } else {
            res  = await fetch(`${API_BASE}/bookings/pnr/${query.toUpperCase()}`);
            json = await res.json();
        }

        if (!res.ok) {
            listEl.innerHTML = `<p style="color:var(--clr-danger);text-align:center;padding:20px">${json.message || 'Not found'}</p>`;
            return;
        }

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
        const json = await res.json();
        if (!res.ok) { showToast(json.message || 'Cancellation failed', 'error'); return; }

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
    toast.textContent = message;
    toast.className   = `toast ${type}`;
    toast.classList.remove('hidden');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.add('hidden'), 4000);
}
