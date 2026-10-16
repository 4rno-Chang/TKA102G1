package com.bistroops.orders.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.member.model.MemberService;
import com.bistroops.orders.ecpay.EcpayUtil;
import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;
import com.bistroops.ordersdetails.model.OrdersDetailsService;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/staff")
public class OrdersController {
	
	@Autowired
	EmployeeService employeeService;

	@Autowired
	OrdersService ordersService;
	
	@Autowired
	OrdersDetailsService ordersDetailsService;
	

	
	@Autowired
	EcpayUtil ecpayUtil; // 綠界小工具（商店資料從 application.properties 讀）
	
	@Value("${ecpay.return-url}")   // 從 application.properties 讀綠界的 ReturnURL
    private String ecpayReturnUrl;
	
	@GetMapping("/orders")
	public String orders(@RequestParam(required =false)String status, //目前狀態
						 @RequestParam(required =false)String keyword, //搜尋關鍵字
						 @RequestParam(required =false)String date, // 日期
						 Model model) {
		LocalDate today=LocalDate.now();
		LocalDate day= today ;
		try{ 
			if (date!=null && !date.isBlank())  
				day = LocalDate.parse(date);  //格式判斷：parse 失敗會丟例外
		}catch(DateTimeParseException e){
			day = today;     // 格式錯就用今天
		}
		if (day.isAfter(today)) //不准選未來
			day=today; 
		
		String weekday ="一二三四五六日".substring(day.getDayOfWeek().getValue()-1,  //返回一個列舉（Enum）型態。
				day.getDayOfWeek().getValue());
		model.addAttribute("status",status == null ?"" : status);
		model.addAttribute("keyword",keyword);
		model.addAttribute("date",day);
		model.addAttribute("dateText",day.format(DateTimeFormatter.ofPattern("MM/dd"))+"("+ weekday +")");
		model.addAttribute("prevDate",day.minusDays(1));//減一天
		model.addAttribute("nextDate",day.isBefore(today)?day.plusDays(1):null);//加一天plusDays,今天的話給 null,前端就不顯示「下一天」按鈕
		model.addAttribute("today",today);
		
		model.addAttribute("orderslistAll", ordersService.search(status, keyword, day));//查資料庫
		return "staff/orders";
	}
	
//===========結帳===============
	@GetMapping("/checkout") //staff/checkout(order=${o.orders})
	public String checkout(@RequestParam(required = false) Integer order, Model model) {
		if(order != null)ordersService.recalculateUnpaid(order);//一進頁面就重新計算總金額
	    model.addAttribute("checkoutListout", ordersService.getUnpaid());
	    if (order != null) model.addAttribute("order", ordersService.getOne(order));
	    return "staff/checkout";
	}
	@PostMapping("/checkout/pay")
	public String pay(@RequestParam Integer order,
					  @RequestParam String method) {
		ordersService.pay(order,method);
		return "redirect:/staff/checkout";
	}

	//==============workboard給畫面==================
	@GetMapping("/workboard")
	public String workboard(
	        @RequestParam(required = false) String view,
	        Model model,
	        HttpSession session) {

	    // 取得目前登入的員工編號
	    Integer empNo = (Integer) session.getAttribute("employeeNo");

	    // 查詢內場與外場權限
	    boolean hasKitchen = employeeService.hasPermission(empNo, 1);
	    boolean hasFloor = employeeService.hasPermission(empNo, 2);

	    // 沒有指定 view，依權限選擇預設畫面
	    if (view == null || view.isBlank()) {

	        if (hasKitchen) {
	            return "redirect:/staff/workboard?view=kitchen";
	        }

	        if (hasFloor) {
	            return "redirect:/staff/workboard?view=floor";
	        }

	        return "redirect:/staff";
	    }

	    // 將權限資訊傳給 HTML
	    model.addAttribute("hasKitchen", hasKitchen);
	    model.addAttribute("hasFloor", hasFloor);	
	    model.addAttribute("view",view); 
		model.addAttribute("workboardlistAll", "done".equals(view)?ordersService.getDone():ordersService.getActive());
		
		return "staff/workboard" ;
	}
	
	//============workboard訂單修改=============
	@GetMapping("/workboard/edit")
	public String workboardEdit( @RequestParam Integer orderId, Model model, HttpSession session) {

	    OrdersVO editOrder = ordersService.getOne(orderId);

	    if (editOrder == null) {
	        return "redirect:/staff/workboard";
	    }

	    // 新增：取得員工編號
	    Integer empNo = (Integer) session.getAttribute("employeeNo");

	    // 新增：提供內場、外場權限給 HTML
	    model.addAttribute("hasKitchen", employeeService.hasPermission(empNo, 1));

	    model.addAttribute("hasFloor", employeeService.hasPermission(empNo, 2));
		
		model.addAttribute("view","kitchen");
		model.addAttribute("workboardlistAll",ordersService.getActive());
		
		model.addAttribute("editOrder",editOrder); //取得明細
		model.addAttribute("canCancel", editOrder.getOrdersDetails().stream()
		            .allMatch(d -> "製作中".equals(d.getOdStatus()) || "已取消".equals(d.getOdStatus())));
		
		return "/staff/workboard";
	}
	//===========輸入會員===========
	@PostMapping("/checkout/member")
	public String  bindMember(@RequestParam Integer order,
							  @RequestParam String phone,
							    RedirectAttributes ra) {
		String error=ordersService.bindMember(order,phone);
		ra.addFlashAttribute("memberMsg",error==null?"已綁定會員" :error);
		return "redirect:/staff/checkout?order="+order;     // 回到同一張訂單
	}
		
	
	
	
	
	
	
    //==============產生綠界表單=====================
    @PostMapping("/checkout/card")
    @ResponseBody
    public String payByCard(@RequestParam Integer order) {
            OrdersVO orders = ordersService.getOne(order);
            String base = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString(); // 現在網站的網址，例如 http://localhost:8080
            Map<String, String> p = new LinkedHashMap<>();
            p.put("MerchantID", ecpayUtil.getMerchantId());
            p.put("MerchantTradeNo", "BO" + order + "T" + System.currentTimeMillis() % 100000); // 英數、20字內、不可重複
            p.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
            p.put("PaymentType", "aio");
            p.put("TotalAmount", String.valueOf(orders.getOrdersActualPrice())); // 必須是整數
            p.put("TradeDesc", "BistroOps");
            p.put("ItemName", "餐點費用");
            p.put("ReturnURL", ecpayReturnUrl);                              // 綠界伺服器通知的網址，從 application.properties 讀
            p.put("OrderResultURL", base + "/staff/checkout/ecpay-result");  // 付款後小視窗跳回的網址，自動用現在的網址
            p.put("ChoosePayment", "Credit"); // 只開信用卡；填 ALL 就是全部付款方式
            p.put("EncryptType", "1");
            p.put("CustomField1", String.valueOf(order)); // 付款完成後用它找回是哪一筆訂單
            return ecpayUtil.buildForm(p);
    }

	// 綠界付款完成後，由綠界的伺服器直接呼叫這裡
	@PostMapping("/checkout/ecpay-return")
	@ResponseBody
	public String ecpayReturn(@RequestParam Map<String, String> params) {
	        String mac = ecpayUtil.checkMacValue(params);
	        if (!mac.equals(params.get("CheckMacValue"))) return "0|CheckMacValue Error"; // 被竄改或偽造的通知
	        if ("1".equals(params.get("RtnCode"))) {
	        	try {
		                ordersService.pay(Integer.valueOf(params.get("CustomField1")), "刷卡");
		        } catch (IllegalArgumentException e) {
		                // 已經結過帳（ecpayResult 先處理了，或綠界重送通知），不用再結
		        }
	        }
	        return "1|OK"; // 一定要回這個字串，不然綠界會一直重送
	}
	// 付款結束後，小視窗會被綠界帶回這裡：顯示結果 → 主視窗的結帳頁重新整理 → 關掉小視窗
	@PostMapping("/checkout/ecpay-result")
	@ResponseBody
	public String ecpayResult(@RequestParam Map<String, String> params) {
	        boolean ok = ecpayUtil.checkMacValue(params).equals(params.get("CheckMacValue"))
	                        && "1".equals(params.get("RtnCode"));
	        if (ok) {
	                try {
	                        ordersService.pay(Integer.valueOf(params.get("CustomField1")), "刷卡");
	                } catch (IllegalArgumentException e) {
	                        // ecpayReturn 已經先結過帳了，不用再結一次
	                }
	        }
	        String msg = ok ? "付款成功，視窗即將關閉…" : "付款失敗：" + params.get("RtnMsg");
	        return "<p style='font-size:20px;text-align:center;margin-top:40px'>" + HtmlUtils.htmlEscape(msg) + "</p>"
	                 + "<script>setTimeout(function () {"
	                 + "  if (window.opener) window.opener.location.href = '/staff/checkout';" // 主視窗回到結帳頁（重新整理）
	                 + "  window.close();"                                                    // 關掉小視窗
	                 + "}, 1500);</script>";
	}
	
		
	
	
}
