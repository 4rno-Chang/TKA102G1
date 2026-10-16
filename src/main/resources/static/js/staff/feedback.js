const dialog = document.getElementById('detailDialog');

document.querySelectorAll('.btn-view').forEach(btn => {
    btn.addEventListener('click', () => {
        const d = btn.dataset;   // 讀取按鈕上的 data-* 屬性
        document.getElementById('d-no').textContent = d.no;
        document.getElementById('d-order').textContent = d.order;
        document.getElementById('d-customer').textContent = d.customer;
        document.getElementById('d-tel').textContent = d.tel;
        document.getElementById('d-dine').textContent = d.dine;
        document.getElementById('d-time').textContent = d.time;
        const r = Number(d.rating) || 0;
        document.getElementById('d-rating').textContent = '★'.repeat(r) + '☆'.repeat(5 - r);
        document.getElementById('d-content').textContent = d.content;
        dialog.showModal();
    });
});

// 關閉彈窗
document.getElementById('btnCloseDialog').addEventListener('click', () => dialog.close());