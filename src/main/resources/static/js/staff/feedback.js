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

        // 訂單明細：把同一格裡 <template class="fb-detail"> 的內容複製一份，換掉上一次顯示的明細
        // （template.content 是裡面的內容；cloneNode(true) 連裡面的子元素一起複製，template 本身不會被拿走，下次還能再用）
        const tpl = btn.parentElement.querySelector('.fb-detail');
        const detail = document.getElementById('d-detail');
        if (tpl) detail.replaceChildren(tpl.content.cloneNode(true));
        else detail.textContent = '沒有訂單明細';
        dialog.showModal();
    });
});

// 關閉彈窗
document.getElementById('btnCloseDialog').addEventListener('click', () => dialog.close());