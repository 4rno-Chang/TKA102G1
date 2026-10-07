/* ========== 1. 手機版漢堡選單 ========== */
const menuToggle = document.getElementById('menuToggle');
const mainNav = document.getElementById('mainNav');

menuToggle.addEventListener('click', () => {
  const isOpen = mainNav.classList.toggle('open');
  menuToggle.classList.toggle('open', isOpen);
  menuToggle.setAttribute('aria-expanded', isOpen);
});

// 點了選單連結後自動收起
mainNav.querySelectorAll('a').forEach(link => {
  link.addEventListener('click', () => {
    mainNav.classList.remove('open');
    menuToggle.classList.remove('open');
    menuToggle.setAttribute('aria-expanded', false);
  });
});

/* ========== 2. 輪播（自動播放 + 圓點 + 左右箭頭） ========== */
const carousel = document.getElementById('carousel');
const track = carousel.querySelector('.carousel-track');
const slides = carousel.querySelectorAll('.slide');
const dotsBox = carousel.querySelector('.carousel-dots');
let current = 0;
let timer = null;

// 依照投影片數量產生圓點
slides.forEach((_, i) => {
  const dot = document.createElement('button');
  dot.setAttribute('aria-label', `第 ${i + 1} 張`);
  dot.addEventListener('click', () => { goTo(i); restart(); });
  dotsBox.appendChild(dot);
});
const dots = dotsBox.querySelectorAll('button');

function goTo(index) {
  current = (index + slides.length) % slides.length;   // 超出範圍時繞回
  track.style.transform = `translateX(-${current * 100}%)`;
  dots.forEach((d, i) => d.classList.toggle('active', i === current));
}
function start()   { timer = setInterval(() => goTo(current + 1), 4000); }
function stop()    { clearInterval(timer); }
function restart() { stop(); start(); }

carousel.querySelector('.prev').addEventListener('click', () => { goTo(current - 1); restart(); });
carousel.querySelector('.next').addEventListener('click', () => { goTo(current + 1); restart(); });
carousel.addEventListener('mouseenter', stop);   // 滑鼠移入暫停
carousel.addEventListener('mouseleave', start);

goTo(0);
start();

/* ========== 3. 常見問題（手風琴） ========== */
// 問答由 frontend_index.html 用 Thymeleaf 從資料庫產生，這裡只負責點擊展開／收起
const faqList = document.getElementById('faqList');

// 事件委派：只在外層掛一個 listener
faqList.addEventListener('click', e => {
  const btn = e.target.closest('.faq-question');
  if (!btn) return;

  const item = btn.parentElement;
  const answer = item.querySelector('.faq-answer');
  const willOpen = !item.classList.contains('open');

  // 一次只展開一題：先把其他題收起
  faqList.querySelectorAll('.faq-item.open').forEach(openItem => {
    openItem.classList.remove('open');
    openItem.querySelector('.faq-answer').style.maxHeight = null;
    openItem.querySelector('.faq-question').setAttribute('aria-expanded', false);
  });

  if (willOpen) {
    item.classList.add('open');
    answer.style.maxHeight = answer.scrollHeight + 'px';
    btn.setAttribute('aria-expanded', true);
  }
});

// 視窗寬度改變時，重新計算已展開答案的高度（避免文字換行後被切掉）
window.addEventListener('resize', () => {
  faqList.querySelectorAll('.faq-item.open .faq-answer').forEach(a => {
    a.style.maxHeight = a.scrollHeight + 'px';
  });
});

/* ========== 4. Header 陰影 + 回到頂端按鈕 ========== */
const header = document.getElementById('siteHeader');
const backToTop = document.getElementById('backToTop');

window.addEventListener('scroll', () => {
  header.classList.toggle('scrolled', window.scrollY > 10);
  backToTop.classList.toggle('show', window.scrollY > 400);
});
backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

/* ========== 5. 捲動到區塊時淡入 ========== */
const observer = new IntersectionObserver(entries => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.15 });
document.querySelectorAll('.reveal').forEach(el => observer.observe(el));

/* ========== 6. 頁尾年份 ========== */
document.getElementById('year').textContent = new Date().getFullYear();

/* ========== 7. 關於我們：點擊切換說明文字（滑鼠移入的效果由 CSS 處理） ========== */
const aboutBox = document.getElementById('aboutBox');

function toggleAbout() {
  const isActive = aboutBox.classList.toggle('active');
  aboutBox.setAttribute('aria-expanded', isActive);
}
aboutBox.addEventListener('click', toggleAbout);
// 鍵盤操作：按 Enter 或空白鍵也能切換
aboutBox.addEventListener('keydown', e => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault();
    toggleAbout();
  }
});

// 回復成「圖片 + 關於我們」
function closeAbout() {
  aboutBox.classList.remove('active');
  aboutBox.setAttribute('aria-expanded', false);
}
// 滑鼠離開區塊時回復
aboutBox.addEventListener('mouseleave', closeAbout);
// 點到區塊以外的地方時回復（手機沒有滑鼠離開的動作，靠這個收起）
document.addEventListener('click', e => {
  if (!aboutBox.contains(e.target)) closeAbout();
});
