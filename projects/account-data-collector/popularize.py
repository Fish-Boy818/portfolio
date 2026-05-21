from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
# 删除webdriver_manager导入
import time
from selenium.common.exceptions import TimeoutException, NoSuchElementException, StaleElementReferenceException, ElementClickInterceptedException
from selenium.webdriver.common.action_chains import ActionChains
from dotenv import load_dotenv
import os

load_dotenv()

ACCOUNT_LOGIN_URL = os.environ.get("ACCOUNT_LOGIN_URL", "").strip()
ACCOUNT_LOGIN_PHONE = os.environ.get("ACCOUNT_LOGIN_PHONE", "").strip()
ACCOUNT_LOGIN_PASSWORD = os.environ.get("ACCOUNT_LOGIN_PASSWORD", "").strip()
WECHAT_WEBHOOK_URL = os.environ.get("WECHAT_WEBHOOK_URL", "").strip()
ENABLE_WECHAT_PUSH = os.environ.get("ENABLE_WECHAT_PUSH", "false").strip().lower() in {"1", "true", "yes", "on"}


def require_env(name, value):
    """Return a required environment value or stop with a clear setup message."""
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value

def ensure_element_clickable(driver, element):
    """确保元素可点击，移除可能的遮盖元素"""
    try:
        # 滚动到元素位置
        driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
        time.sleep(0.5)  # 等待滚动完成

        # 尝试移除可能的遮盖元素（如弹窗、遮罩层等）
        driver.execute_script("""
            // 移除可能的遮盖层
            var overlays = document.querySelectorAll('.modal, .overlay, .dialog, [class*="mask"], [class*="popup"], [class*="modal"], [class*="drawer"]');
            overlays.forEach(function(overlay) {
                if (overlay && overlay.style) {
                    overlay.style.display = 'none';
                }
            });
            
            // 移除可能的fixed或absolute定位的元素
            var fixedElements = document.querySelectorAll('div[style*="position: fixed"], div[style*="position:fixed"], div[style*="position: absolute"], div[style*="position:absolute"]');
            fixedElements.forEach(function(el) {
                if (el && el.style) {
                    el.style.display = 'none';
                }
            });
            
            // 确保元素可见
            if (arguments[0]) {
                arguments[0].style.zIndex = '9999';
                arguments[0].style.position = 'relative';
                arguments[0].style.opacity = '1';
                arguments[0].style.visibility = 'visible';
                arguments[0].style.pointerEvents = 'auto';
            }
        """, element)

        # 再次滚动确保元素在视图中
        driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
        time.sleep(0.5)  # 等待滚动完成

        return True
    except Exception as e:
        print(f"确保元素可点击时出错: {str(e)}")
        return False

def retry_on_stale_element(driver, find_func, action_func, max_retries=3, wait_time=1):
    """
    处理stale element错误的通用重试函数
    
    Args:
        driver: WebDriver实例
        find_func: 查找元素的函数，接收driver作为参数，返回元素
        action_func: 对元素执行操作的函数，接收元素作为参数
        max_retries: 最大重试次数
        wait_time: 每次重试前等待的时间(秒)
        
    Returns:
        bool: 操作是否成功
    """
    retries = 0
    while retries < max_retries:
        try:
            # 移除可能的遮盖层
            remove_overlays(driver)
            
            # 查找元素
            element = find_func(driver)
            if element:
                # 执行操作
                return action_func(element)
            else:
                print(f"未找到元素，第 {retries + 1} 次重试...")
        except StaleElementReferenceException:
            print(f"元素已过期，第 {retries + 1} 次重试...")
        except Exception as e:
            print(f"操作失败: {str(e)}，第 {retries + 1} 次重试...")
        
        retries += 1
        time.sleep(wait_time)  # 重试前等待
    
    print(f"操作失败，已达到最大重试次数 {max_retries}")
    return False

def safe_click(driver, element):
    """安全点击元素，处理可能的点击拦截"""
    try:
        # 首先尝试确保元素可点击
        ensure_element_clickable(driver, element)

        # 尝试常规点击
        try:
            element.click()
            return True
        except ElementClickInterceptedException:
            # 如果点击被拦截，尝试使用JavaScript点击
            driver.execute_script("arguments[0].click();", element)
            return True
        except StaleElementReferenceException:
            print("元素已过期，无法点击")
            return False
        except Exception as e:
            print(f"常规点击失败: {str(e)}")

            # 尝试使用JavaScript点击
            try:
                driver.execute_script("arguments[0].click();", element)
                return True
            except StaleElementReferenceException:
                print("元素已过期，无法通过JavaScript点击")
                return False
            except Exception as e:
                print(f"JavaScript点击失败: {str(e)}")

                # 尝试使用ActionChains点击
                try:
                    actions = ActionChains(driver)
                    actions.move_to_element(element).click().perform()
                    return True
                except StaleElementReferenceException:
                    print("元素已过期，无法通过ActionChains点击")
                    return False
                except Exception as e:
                    print(f"ActionChains点击失败: {str(e)}")
                    return False
    except Exception as e:
        print(f"安全点击操作失败: {str(e)}")
        return False

def get_current_account_name(driver):
    """
    获取当前账户名称
    
    Args:
        driver: WebDriver实例
        
    Returns:
        str: 当前账户名称，如果获取失败则返回"未知账户"
    """
    try:
        # 方法1: 通过账户信息区域获取
        try:
            account_name = WebDriverWait(driver, 5).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, "div.navigator-user-account-info-title span.oc-typography-value-int"))
            )
            name_text = account_name.text
            if name_text:
                print(f"通过账户信息区域获取到账户名称: {name_text}")
                return name_text
        except:
            pass
        
        # 方法2: 尝试其他选择器
        selectors = [
            "span.oc-typography-value-int",
            "div.current-account span.oc-typography-value-int",
            "div.navigator-user-button span.oc-typography-value-int"
        ]
        
        for selector in selectors:
            try:
                account_name = driver.find_element(By.CSS_SELECTOR, selector)
                name_text = account_name.text
                if name_text and "ID：" not in name_text and len(name_text) > 1:
                    print(f"通过选择器 '{selector}' 获取到账户名称: {name_text}")
                    return name_text
            except:
                continue
        
        # 方法3: 通过标题获取
        try:
            title = driver.title
            if "账户" in title and "-" in title:
                account_name = title.split("-")[0].strip()
                print(f"通过页面标题获取到账户名称: {account_name}")
                return account_name
        except:
            pass
        
        print("无法获取当前账户名称")
        return "未知账户"
    except Exception as e:
        print(f"获取当前账户名称时出错: {str(e)}")
        return "未知账户"

def collect_account_data(driver):
    """收集当前账户的数据"""
    try:
        # 获取当前账户名称
        account_name = get_current_account_name(driver)
        
        # 点击"账户整体消耗(元)"卡片并获取相关数据
        try:
            print("尝试点击'账户整体消耗(元)'卡片...")
            
            # 尝试定位"账户整体消耗(元)"卡片
            try:
                # 方法1：通过文本内容定位
                consumption_card = WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.XPATH, "//div[contains(@class, 'report-metrics-calculate-tab-card')]//span[contains(text(), '账户整体消耗')]"))
                )
                # 使用安全点击方法
                if safe_click(driver, consumption_card):
                    print("已点击'账户整体消耗(元)'卡片")
                else:
                    print("点击'账户整体消耗(元)'卡片失败")
            except Exception as e:
                print(f"通过文本内容定位'账户整体消耗(元)'卡片失败: {str(e)}")

                # 方法2：通过类名和样式属性定位
                try:
                    card = WebDriverWait(driver, 5).until(
                        EC.presence_of_element_located((By.CSS_SELECTOR, "div.report-metrics-calculate-tab-card[style*='border-left: 0px'][style*='border-radius: 16px 0px 0px']"))
                    )
                    # 使用安全点击方法
                    if safe_click(driver, card):
                        print("已通过类名和样式属性点击卡片")
                    else:
                        print("通过类名和样式属性点击卡片失败")
                except Exception as e:
                    print(f"通过类名和样式属性定位卡片失败: {str(e)}")

                    # 方法3：尝试定位所有卡片，然后找到第一个
                    try:
                        cards = driver.find_elements(By.CSS_SELECTOR, "div.report-metrics-calculate-tab-card")
                        if cards:
                            # 使用安全点击方法
                            if safe_click(driver, cards[0]):
                                print("已点击第一个卡片")
                            else:
                                print("点击第一个卡片失败")
                    except Exception as e:
                        print(f"尝试点击第一个卡片失败: {str(e)}")
            
            # 等待数据加载
            print("等待数据加载...")
            time.sleep(3)
            
            # 获取当前账户名称
            try:
                account_name = driver.find_element(By.CSS_SELECTOR, "div.oc-item-group-content span.oc-typography-value-int").text
                print(f"\n当前账户: {account_name}")
            except Exception as e:
                print(f"获取当前账户名称失败: {str(e)}")
                account_name = "未知账户"
            
            # 收集和显示数据
            try:
                # 收集"账户整体消耗(元)"数据
                try:
                    consumption_value = WebDriverWait(driver, 5).until(
                        EC.presence_of_element_located((By.XPATH, "//div[contains(@class, 'report-metrics-calculate-tab-card-value')]//span[contains(@class, 'oc-typography-value-int oc-typography-value-slot')]"))
                    ).text
                    print(f"账户整体消耗(元): {consumption_value}")
                except Exception as e:
                    print(f"获取账户整体消耗数据失败: {str(e)}")
                
                # 收集其他数据卡片信息
                data_cards = driver.find_elements(By.CSS_SELECTOR, "div.oc-promotion-metric-card")
                print("\n收集到的数据:")
                
                for card in data_cards:
                    try:
                        # 获取卡片标题
                        title = card.find_element(By.XPATH, ".//span[contains(@class, 'oc-typography-value-int') and contains(@elementtiming, 'home-element-timing')]").text
                        
                        # 获取卡片数值
                        value_elements = card.find_elements(By.XPATH, ".//div[contains(@class, 'oc-promotion-metric-card-number')]//span[contains(@class, 'oc-typography-value-int')]")
                        value = ""
                        for element in value_elements:
                            value += element.text
                        
                        print(f"{title}: {value}")
                    except Exception as e:
                        continue
                
                # 获取账户总余额
                try:
                    balance_title = driver.find_element(By.XPATH, "//div[contains(@class, 'account-title')]/span").text
                    balance_value = driver.find_element(By.XPATH, "//div[contains(@class, 'account-money')]").text
                    print(f"{balance_title}: {balance_value}")
                except Exception as e:
                    print(f"获取账户总余额失败: {str(e)}")
                
                return True
                
            except Exception as e:
                print(f"收集数据失败: {str(e)}")
                return False
            
        except Exception as e:
            print(f"点击'账户整体消耗(元)'卡片失败: {str(e)}")
            return False
            
    except Exception as e:
        print(f"处理账户数据时发生错误: {str(e)}")
        return False

def scroll_to_element(driver, element):
    """滚动到元素位置，确保元素在视图中可见"""
    try:
        driver.execute_script("arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", element)
        time.sleep(0.5)  # 等待滚动完成
        return True
    except Exception as e:
        print(f"滚动到元素位置失败: {str(e)}")
        return False

def scroll_list(driver, container_selector, amount=300):
    """在列表容器中滚动指定的像素数"""
    try:
        script = f"""
            var container = document.querySelector('{container_selector}');
            if (container) {{
                container.scrollTop += {amount};
                return container.scrollTop;
            }}
            return -1;
        """
        scroll_position = driver.execute_script(script)
        time.sleep(0.5)  # 等待滚动完成
        return scroll_position
    except Exception as e:
        print(f"列表滚动失败: {str(e)}")
        return -1

def remove_overlays(driver):
    """
    移除可能的弹窗、遮罩层等阻碍元素点击的元素
    
    Args:
        driver: WebDriver实例
    """
    try:
        # 使用JavaScript移除可能的遮盖层
        driver.execute_script("""
            // 移除可能的遮盖层
            var overlays = document.querySelectorAll('.modal, .overlay, .dialog, [class*="mask"], [class*="popup"], [class*="modal"], [class*="drawer"]');
            overlays.forEach(function(overlay) {
                if (overlay && overlay.style) {
                    overlay.style.display = 'none';
                }
            });
            
            // 移除可能的fixed或absolute定位的元素
            var fixedElements = document.querySelectorAll('div[style*="position: fixed"], div[style*="position:fixed"], div[style*="position: absolute"], div[style*="position:absolute"]');
            fixedElements.forEach(function(el) {
                if (el && el.style) {
                    el.style.display = 'none';
                }
            });
        """)
        print("已尝试移除可能的遮盖层")
    except Exception as e:
        print(f"移除遮盖层时出错: {str(e)}")

def check_if_need_switch_account(driver):
    """
    检查是否需要切换账户，有些页面可能已经在账户列表页面
    
    Args:
        driver: WebDriver实例
        
    Returns:
        bool: 是否需要切换账户
    """
    try:
        # 检查页面上是否有账户列表元素
        try:
            account_elements = driver.find_elements(By.CSS_SELECTOR, ".account-card")
            if account_elements:
                print("页面上已有账户列表元素，无需切换账户")
                return False
        except:
            pass
            
        # 检查是否有账户切换按钮
        try:
            account_info = driver.find_element(By.CSS_SELECTOR, "div.navigator-user-button")
            if account_info:
                print("找到账户信息区域，需要切换账户")
                return True
        except:
            pass
            
        # 默认需要切换账户
        return True
    except Exception as e:
        print(f"检查是否需要切换账户时出错: {str(e)}")
        return True

def find_account_elements(driver):
    """
    尝试多种方法查找页面上的账户元素
    
    Args:
        driver: WebDriver实例
        
    Returns:
        list: 找到的账户元素列表
        str: 使用的选择器
    """
    # 尝试多种方式定位账户列表
    account_elements = []
    selector_used = ""
    
    # 方法1: 尝试定位账户列表页面中的账户卡片
    try:
        account_elements = driver.find_elements(By.CSS_SELECTOR, ".account-card")
        if account_elements:
            print("通过.account-card选择器找到账户元素")
            selector_used = ".account-card"
            return account_elements, selector_used
    except:
        print("未找到.account-card元素，尝试其他方法")
    
    # 方法2: 尝试定位无限滚动列表中的账户
    try:
        account_list_container = WebDriverWait(driver, 5).until(
            EC.presence_of_element_located((By.CLASS_NAME, "ovui-infinite-scroll"))
        )
        account_elements = account_list_container.find_elements(By.XPATH, ".//div[contains(@class, 'ovui-infinite-scroll-item')]")
        if account_elements:
            print("通过ovui-infinite-scroll找到账户元素")
            selector_used = ".ovui-infinite-scroll-item"
            return account_elements, selector_used
    except:
        print("未找到无限滚动列表，尝试其他方法")
    
    # 方法3: 尝试查找下拉菜单中的账户列表
    try:
        dropdown_menu = WebDriverWait(driver, 5).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, ".oc-dropdown-menu"))
        )
        account_elements = dropdown_menu.find_elements(By.CSS_SELECTOR, ".oc-dropdown-menu-item")
        if account_elements:
            print("通过.oc-dropdown-menu-item选择器找到账户元素")
            selector_used = ".oc-dropdown-menu-item"
            return account_elements, selector_used
    except:
        print("未找到下拉菜单中的账户列表，尝试其他方法")
    
    # 方法4: 尝试更通用的选择器
    try:
        account_elements = driver.find_elements(By.CSS_SELECTOR, "[role='button']")
        if account_elements:
            print("通过[role='button']选择器找到账户元素")
            selector_used = "[role='button']"
            return account_elements, selector_used
    except:
        print("未找到[role='button']元素，尝试其他方法")
    
    # 方法5: 尝试查找所有可能的账户元素
    try:
        account_elements = driver.find_elements(By.CSS_SELECTOR, "div.ovui-infinite-scroll > div")
        if account_elements:
            print("通过div.ovui-infinite-scroll > div选择器找到账户元素")
            selector_used = "div.ovui-infinite-scroll > div"
            return account_elements, selector_used
    except:
        print("未找到div.ovui-infinite-scroll > div元素，尝试其他方法")
        
    # 方法6: 尝试查找表格中的账户行
    try:
        account_elements = driver.find_elements(By.CSS_SELECTOR, "tr.oc-table-row")
        if account_elements:
            print("通过tr.oc-table-row选择器找到账户元素")
            selector_used = "tr.oc-table-row"
            return account_elements, selector_used
    except:
        print("未找到tr.oc-table-row元素，尝试其他方法")
    
    # 方法7: 尝试查找可能的账户列表项
    try:
        account_elements = driver.find_elements(By.CSS_SELECTOR, "div.account-item")
        if account_elements:
            print("通过div.account-item选择器找到账户元素")
            selector_used = "div.account-item"
            return account_elements, selector_used
    except:
        print("未找到div.account-item元素，尝试其他方法")
    
    # 方法8: 尝试查找菜单中的账户选项
    try:
        account_elements = driver.find_elements(By.XPATH, "//*[contains(text(), '账户') and (@role='menuitem' or contains(@class, 'menu') or contains(@class, 'item'))]")
        if account_elements:
            print("通过包含'账户'文本的菜单项找到账户元素")
            selector_used = "账户菜单项"
            return account_elements, selector_used
    except:
        print("未找到包含'账户'文本的菜单项，尝试其他方法")
        
    return [], ""

def click_switch_button(driver):
    """
    点击账户切换按钮，使用用户提供的HTML结构中的精确选择器
    
    Args:
        driver: WebDriver实例
        
    Returns:
        bool: 是否成功点击切换按钮
    """
    try:
        print("尝试点击账户切换按钮...")
        
        # 尝试直接点击导航栏中的账户按钮
        try:
            account_button = WebDriverWait(driver, 10).until(
                EC.element_to_be_clickable((By.CSS_SELECTOR, "div.navigator-user-button"))
            )
            driver.execute_script("arguments[0].click();", account_button)
            print("已点击账户按钮")
            time.sleep(2)  # 等待点击效果
            return True
        except Exception as e:
            print(f"点击账户按钮失败: {str(e)}")
            
            # 尝试通过多种选择器定位切换按钮
            selectors = [
                "//div[contains(@class, 'navigator-user-button')]",
                "//button[contains(@class, 'navigator-user-account-info-switch')]",
                "//button[.//span[contains(text(), '切换')]]",
                "//span[contains(text(), '切换')]/parent::button"
            ]
            
            for selector in selectors:
                try:
                    switch_button = WebDriverWait(driver, 3).until(
                        EC.element_to_be_clickable((By.XPATH, selector))
                    )
                    driver.execute_script("arguments[0].click();", switch_button)
                    print(f"已通过选择器 '{selector}' 点击切换按钮")
                    time.sleep(2)
                    return True
                except:
                    continue
            
            print("所有备用选择器都失败了")
            return False
    except Exception as e:
        print(f"点击账户切换按钮过程中出错: {str(e)}")
        return False

def extract_account_id_from_element(element):
    """
    从账户元素中提取账户ID
    
    Args:
        element: 账户元素
        
    Returns:
        str: 账户ID，如果无法提取则返回空字符串
    """
    try:
        # 方法1: 直接查找包含"ID："文本的元素
        try:
            id_span = element.find_element(By.XPATH, ".//span[contains(@class, 'oc-typography-value-int') and contains(text(), 'ID：')]")
            if id_span:
                text = id_span.text
                if "ID：" in text:
                    id_part = text.split("ID：")[1].strip()
                    # 如果ID后面还有其他文本，只取数字部分
                    import re
                    match = re.search(r'(\d+)', id_part)
                    if match:
                        return match.group(1)
                    return id_part
        except:
            pass
        
        # 方法2: 查找所有oc-typography-value-int类的span元素
        try:
            spans = element.find_elements(By.CSS_SELECTOR, "span.oc-typography-value-int")
            for span in spans:
                text = span.text
                if "ID：" in text:
                    id_part = text.split("ID：")[1].strip()
                    # 如果ID后面还有其他文本，只取数字部分
                    import re
                    match = re.search(r'(\d+)', id_part)
                    if match:
                        return match.group(1)
                    return id_part
        except:
            pass
        
        # 方法3: 尝试从元素文本中提取ID
        try:
            text = element.text
            if "ID：" in text or "ID:" in text:
                separator = "ID：" if "ID：" in text else "ID:"
                lines = text.split('\n')
                for line in lines:
                    if separator in line:
                        id_part = line.split(separator)[1].strip()
                        # 如果ID后面还有其他文本，只取数字部分
                        import re
                        match = re.search(r'(\d+)', id_part)
                        if match:
                            return match.group(1)
                        return id_part
        except:
            pass
        
        # 方法4: 尝试从属性中提取ID
        id_attr = element.get_attribute("data-account-id") or element.get_attribute("id") or ""
        if id_attr and id_attr.isdigit():
            return id_attr
        
        return ""
    except Exception as e:
        print(f"从账户元素提取ID失败: {str(e)}")
        return ""

def extract_current_account_id(driver):
    """
    从当前页面提取账户ID
    
    Args:
        driver: WebDriver实例
        
    Returns:
        str: 当前账户ID，如果无法提取则返回空字符串
    """
    try:
        # 方法1: 尝试从current-account元素中提取ID
        try:
            # 首先尝试悬停在账户名称上以显示账户信息
            try:
                account_name_span = driver.find_element(By.CSS_SELECTOR, "span.oc-typography-value-int:not(.oc-typography-value-slot)")
                actions = ActionChains(driver)
                actions.move_to_element(account_name_span).perform()
                time.sleep(1)  # 等待悬停效果显示
            except:
                pass
            
            # 查找current-account元素
            current_account = WebDriverWait(driver, 3).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, "div.current-account"))
            )
            
            # 在current-account元素中查找包含ID的span
            id_span = current_account.find_element(By.XPATH, ".//span[contains(@class, 'oc-typography-value-int') and contains(text(), 'ID：')]")
            if id_span:
                text = id_span.text
                if "ID：" in text:
                    id_part = text.split("ID：")[1].strip()
                    # 如果ID后面还有其他文本，只取数字部分
                    import re
                    match = re.search(r'(\d+)', id_part)
                    if match:
                        print(f"从current-account元素中提取到账户ID: {match.group(1)}")
                        return match.group(1)
                    return id_part
        except Exception as e:
            print(f"从current-account元素中提取ID失败: {str(e)}")
        
        # 方法2: 从URL中提取
        current_url = driver.current_url
        import re
        account_id_match = re.search(r'account_id=(\d+)', current_url)
        if account_id_match:
            print(f"从URL中提取到账户ID: {account_id_match.group(1)}")
            return account_id_match.group(1)
        
        # 方法3: 从页面中查找所有包含"ID："的元素
        try:
            id_elements = driver.find_elements(By.XPATH, "//*[contains(text(), 'ID：')]")
            for element in id_elements:
                text = element.text
                if "ID：" in text:
                    id_part = text.split("ID：")[1].strip()
                    # 如果ID后面还有其他文本，只取数字部分
                    match = re.search(r'(\d+)', id_part)
                    if match:
                        print(f"从页面元素中提取到账户ID: {match.group(1)}")
                        return match.group(1)
        except:
            pass
        
        print("无法提取当前账户ID")
        return ""
    except Exception as e:
        print(f"提取当前账户ID失败: {str(e)}")
        return ""

def switch_account_by_url(driver, account_id):
    """
    通过修改URL直接切换到指定账户
    
    Args:
        driver: WebDriver实例
        account_id: 目标账户ID
        
    Returns:
        bool: 是否成功切换账户
    """
    try:
        print(f"尝试通过URL直接切换到账户ID: {account_id}")
        
        # 获取当前URL
        current_url = driver.current_url
        
        # 检查URL中是否已有account_id参数
        if "account_id=" in current_url:
            # 替换account_id参数
            import re
            new_url = re.sub(r'account_id=\d+', f'account_id={account_id}', current_url)
        else:
            # 添加account_id参数
            separator = "&" if "?" in current_url else "?"
            new_url = f"{current_url}{separator}account_id={account_id}"
        
        # 导航到新URL
        driver.get(new_url)
        print(f"已导航到新URL: {new_url}")
        
        # 等待页面加载
        print("等待页面加载完成...")
        time.sleep(5)
        
        # 检查是否成功切换账户
        try:
            # 等待页面加载完成的标志性元素
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, "div.oc-item-group-content"))
            )
            
            # 获取当前账户ID并验证
            current_id = extract_current_account_id(driver)
            if current_id == account_id:
                print(f"成功切换到账户ID: {account_id}")
                return True
            else:
                print(f"账户ID不匹配，当前ID: {current_id}，目标ID: {account_id}")
                return False
        except Exception as e:
            print(f"验证账户切换失败: {str(e)}")
            return False
    except Exception as e:
        print(f"通过URL切换账户失败: {str(e)}")
        return False

def get_all_account_ids(driver):
    """
    获取所有可用的账户ID列表
    
    Args:
        driver: WebDriver实例
        
    Returns:
        list: 账户ID列表
    """
    try:
        print("尝试获取所有账户ID...")
        
        # 首先尝试点击账户切换按钮
        if not click_switch_button(driver):
            print("点击账户切换按钮失败，无法获取账户列表")
            return []
        
        # 等待账户列表加载
        time.sleep(3)
        
        # 查找账户元素
        account_elements, _ = find_account_elements(driver)
        
        if not account_elements:
            print("未找到账户元素，尝试其他方法获取账户ID")
            return []
        
        # 从账户元素中提取ID
        account_ids = []
        for element in account_elements:
            account_id = extract_account_id_from_element(element)
            if account_id:
                account_ids.append(account_id)
        
        print(f"找到 {len(account_ids)} 个账户ID: {account_ids}")
        return account_ids
    except Exception as e:
        print(f"获取所有账户ID失败: {str(e)}")
        return []

def get_all_accounts_from_page_source(driver):
    """
    从页面源码中提取所有可用账户的信息
    
    Args:
        driver: WebDriver实例
        
    Returns:
        list: 账户信息列表，每个元素是包含id和name的字典
    """
    try:
        print("尝试从页面源码中提取所有账户信息...")
        
        # 获取页面源码
        page_source = driver.page_source
        
        # 使用正则表达式提取账户信息
        import re
        
        # 尝试提取账户ID
        id_pattern = re.compile(r'ID[：:]\s*(\d+)')
        account_ids = id_pattern.findall(page_source)
        print(f"从页面源码中提取到 {len(account_ids)} 个账户ID")
        
        # 尝试提取账户名称 - 使用更精确的模式
        # 1. 尝试匹配可能的账户名称标签
        name_patterns = [
            re.compile(r'oc-typography-value-int">([^<]+)</span>'),  # 基本模式
            re.compile(r'navigator-user-account-info-title[^>]*>.*?oc-typography-value-int">([^<]+)</span>'),  # 标题中的名称
            re.compile(r'account-name[^>]*>([^<]+)<'),  # 账户名称类
            re.compile(r'account-title[^>]*>([^<]+)<')   # 账户标题类
        ]
        
        all_names = []
        for pattern in name_patterns:
            names = pattern.findall(page_source)
            all_names.extend(names)
        
        # 过滤无效的名称
        valid_names = []
        for name in all_names:
            name = name.strip()
            # 跳过无效名称
            if (name and 
                "当前页面访问异常" not in name and 
                "ID：" not in name and 
                "ID:" not in name and
                len(name) > 1):  # 避免单个字符的名称
                valid_names.append(name)
        
        print(f"从页面源码中提取到 {len(valid_names)} 个有效账户名称")
        
        # 创建账户信息列表
        accounts = []
        
        # 首先尝试将ID和名称一一对应
        for i in range(min(len(account_ids), len(valid_names))):
            accounts.append({
                'id': account_ids[i],
                'name': valid_names[i]
            })
        
        # 如果账户ID比有效名称多，使用ID创建剩余账户
        if len(account_ids) > len(accounts):
            for i in range(len(accounts), len(account_ids)):
                accounts.append({
                    'id': account_ids[i],
                    'name': f"账户_{account_ids[i]}"
                })
        
        # 如果有效名称比ID多，使用名称创建剩余账户
        elif len(valid_names) > len(accounts):
            for i in range(len(accounts), len(valid_names)):
                accounts.append({
                    'id': f"unknown_{i+1}",
                    'name': valid_names[i]
                })
        
        print(f"成功提取 {len(accounts)} 个账户信息")
        return accounts
    except Exception as e:
        print(f"从页面源码中提取账户信息失败: {str(e)}")
        return []

def get_accounts_from_api(driver):
    """
    尝试通过API获取所有账户信息
    
    Args:
        driver: WebDriver实例
        
    Returns:
        list: 账户信息列表，每个元素是包含id和name的字典
    """
    try:
        print("尝试通过API获取账户信息...")
        
        # 执行JavaScript获取账户列表
        # 注意：这个JavaScript代码需要根据实际网站的API调用进行修改
        accounts_data = driver.execute_script("""
            // 尝试从localStorage或sessionStorage中获取账户信息
            let accounts = [];
            try {
                // 尝试从localStorage中获取
                const localData = localStorage.getItem('accountList') || localStorage.getItem('accounts');
                if (localData) {
                    accounts = JSON.parse(localData);
                }
            } catch (e) {
                console.error('从localStorage获取账户信息失败:', e);
            }
            
            if (!accounts || accounts.length === 0) {
                try {
                    // 尝试从sessionStorage中获取
                    const sessionData = sessionStorage.getItem('accountList') || sessionStorage.getItem('accounts');
                    if (sessionData) {
                        accounts = JSON.parse(sessionData);
                    }
                } catch (e) {
                    console.error('从sessionStorage获取账户信息失败:', e);
                }
            }
            
            // 如果还没有找到账户信息，尝试从window对象中查找
            if (!accounts || accounts.length === 0) {
                // 遍历window对象的所有属性，查找可能包含账户信息的数据
                for (const key in window) {
                    try {
                        const value = window[key];
                        if (value && typeof value === 'object') {
                            // 检查是否包含账户相关的属性
                            if (value.accounts || value.accountList || value.userAccounts) {
                                accounts = value.accounts || value.accountList || value.userAccounts;
                                break;
                            }
                        }
                    } catch (e) {
                        // 忽略访问某些属性时可能出现的错误
                    }
                }
            }
            
            return accounts;
        """)
        
        if accounts_data and isinstance(accounts_data, list) and len(accounts_data) > 0:
            print(f"通过API成功获取到 {len(accounts_data)} 个账户信息")
            
            # 格式化账户信息
            accounts = []
            for account in accounts_data:
                # 尝试从不同的可能属性中提取ID和名称
                account_id = None
                account_name = None
                
                if isinstance(account, dict):
                    # 尝试不同可能的ID字段名
                    for id_field in ['id', 'accountId', 'account_id', 'ID']:
                        if id_field in account and account[id_field]:
                            account_id = str(account[id_field])
                            break
                    
                    # 尝试不同可能的名称字段名
                    for name_field in ['name', 'accountName', 'account_name', 'title', 'displayName']:
                        if name_field in account and account[name_field]:
                            account_name = account[name_field]
                            break
                
                if account_id:
                    accounts.append({
                        'id': account_id,
                        'name': account_name or f"账户_{account_id}"
                    })
            
            print(f"成功格式化 {len(accounts)} 个账户信息")
            return accounts
        else:
            print("通过API未获取到有效的账户信息")
            return []
    except Exception as e:
        print(f"通过API获取账户信息失败: {str(e)}")
        return []

def get_all_available_accounts(driver):
    """
    获取所有可用账户的信息，尝试多种方法
    
    Args:
        driver: WebDriver实例
        
    Returns:
        list: 账户信息列表，每个元素是包含id和name的字典
    """
    # 首先尝试通过API获取
    accounts = get_accounts_from_api(driver)
    
    # 如果API方法失败，尝试从页面源码中提取
    if not accounts:
        accounts = get_all_accounts_from_page_source(driver)
    
    # 如果仍然没有获取到账户信息，尝试点击切换按钮并从账户列表页面提取
    if not accounts:
        # 点击切换按钮
        if click_switch_button(driver):
            # 等待账户列表加载
            time.sleep(3)
            
            # 从页面源码中提取账户信息
            accounts = get_all_accounts_from_page_source(driver)
    
    # 打印找到的所有账户信息
    if accounts:
        print("\n找到以下账户:")
        for i, account in enumerate(accounts):
            print(f"{i+1}. ID: {account['id']}, 名称: {account['name']}")
    else:
        print("未找到任何可用账户")
    
    return accounts

def switch_to_next_account(driver, processed_indices=None, processed_ids=None, processed_names=None):
    """切换到下一个账户
    
    Args:
        driver: WebDriver实例
        processed_indices: 已处理的账户索引集合，用于避免重复选择
        processed_ids: 已处理的账户ID集合，用于避免重复选择
        processed_names: 已处理的账户名称集合，用于避免重复选择
    """
    if processed_indices is None:
        processed_indices = set()
    
    if processed_ids is None:
        processed_ids = set()
    
    if processed_names is None:
        processed_names = set()
        
    try:
        print("\n尝试切换账户...")
        
        # 获取当前账户名称
        current_account_name = get_current_account_name(driver)
        if current_account_name and current_account_name != "未知账户":
            print(f"当前账户名称: {current_account_name}")
            processed_names.add(current_account_name)
        
        # 点击切换按钮
        if click_switch_button(driver):
            # 等待账户列表加载
            print("等待账户列表加载...")
            time.sleep(3)
            
            # 查找账户元素
            account_elements = driver.find_elements(By.CSS_SELECTOR, "div.oc-item-group")
            
            # 如果没有找到账户元素，尝试其他选择器
            if not account_elements:
                selectors = [
                    "div.account-item", 
                    "div.ovui-infinite-scroll-item",
                    "div.oc-dropdown-menu-item",
                    "div.account-card"
                ]
                
                for selector in selectors:
                    try:
                        account_elements = driver.find_elements(By.CSS_SELECTOR, selector)
                        if account_elements:
                            print(f"通过选择器 '{selector}' 找到账户元素")
                            break
                    except:
                        continue
            
            if account_elements:
                print(f"找到 {len(account_elements)} 个账户")
                
                # 查找下一个未处理的账户
                next_account_index = -1
                for i, account in enumerate(account_elements):
                    try:
                        account_text = account.text
                        # 如果账户名称不在已处理列表中，选择该账户
                        if account_text and not any(name in account_text for name in processed_names):
                            next_account_index = i
                            break
                    except:
                        continue
                
                # 如果找不到未处理的账户，选择第一个账户
                if next_account_index == -1:
                    print("没有找到未处理的账户，选择第一个账户")
                    next_account_index = 0
                
                print(f"选择账户，索引为 {next_account_index}")
                
                # 使用click_account_in_list函数点击账户
                if click_account_in_list(driver, next_account_index):
                    # 等待页面加载
                    print("等待页面加载完成...")
                    time.sleep(8)
                    
                    # 记录已处理的账户
                    processed_indices.add(next_account_index)
                    
                    # 获取并记录新账户的名称
                    new_account_name = get_current_account_name(driver)
                    if new_account_name and new_account_name != "未知账户":
                        processed_names.add(new_account_name)
                        print(f"已切换到新账户: {new_account_name}")
                    
                    return True, processed_indices, processed_ids, processed_names
                else:
                    print("点击账户失败")
                    return False, processed_indices, processed_ids, processed_names
            else:
                print("未找到账户元素")
                return False, processed_indices, processed_ids, processed_names
        else:
            print("点击切换按钮失败")
            return False, processed_indices, processed_ids, processed_names
    except Exception as e:
        print(f"切换账户操作失败: {str(e)}")
        return False, processed_indices, processed_ids, processed_names

def process_all_accounts(driver, max_accounts=None):
    """处理所有账户，如果max_accounts为None则处理所有可用账户"""
    processed_count = 0
    processed_indices = set()  # 用于跟踪已处理的账户索引
    processed_ids = set()      # 用于跟踪已处理的账户ID
    processed_names = set()    # 用于跟踪已处理的账户名称
    
    # 设置最大处理账户数量
    if max_accounts is None:
        max_accounts = 5  # 默认处理5个账户
    
    # 先尝试直接点击切换按钮，如果存在的话
    try:
        print("\n尝试直接点击切换按钮...")
        # 尝试多种方式定位切换按钮
        selectors = [
            "//span[@class='button-text' and text()='切换']",
            "//button[contains(@class, 'navigator-user-account-info-switch')]",
            "//button[.//span[contains(text(), '切换')]]",
            "//div[contains(@class, 'navigator-user-button')]",
            "//div[contains(@class, 'navigator-user-account-info')]"
        ]
        
        for selector in selectors:
            try:
                element = WebDriverWait(driver, 3).until(
                    EC.element_to_be_clickable((By.XPATH, selector))
                )
                driver.execute_script("arguments[0].click();", element)
                print(f"已通过选择器 '{selector}' 点击切换按钮或账户区域")
                time.sleep(2)
                
                # 检查是否出现了账户列表
                try:
                    account_elements = driver.find_elements(By.CSS_SELECTOR, "div.navigator-account")
                    if account_elements and len(account_elements) > 0:
                        print(f"成功打开账户列表，找到 {len(account_elements)} 个账户")
                        break
                except:
                    pass
            except:
                continue
    except Exception as e:
        print(f"尝试直接点击切换按钮失败: {str(e)}")
    
    # 如果直接点击切换按钮失败，尝试先点击账户名称
    try:
        # 检查是否已经显示账户列表
        account_elements = driver.find_elements(By.CSS_SELECTOR, "div.navigator-account")
        if not account_elements or len(account_elements) == 0:
            print("\n尝试点击账户名称...")
            
            # 尝试多种方式定位账户名称
            name_selectors = [
                "span.oc-typography-value-int",
                "div.navigator-user-account-info-title span",
                "div.navigator-user-button span",
                "div.current-account span",
                "//span[contains(@class, 'oc-typography-value-int') and string-length(text()) > 1]"
            ]
            
            for selector in name_selectors:
                try:
                    if selector.startswith("//"):
                        elements = driver.find_elements(By.XPATH, selector)
                    else:
                        elements = driver.find_elements(By.CSS_SELECTOR, selector)
                    
                    for element in elements:
                        try:
                            text = element.text
                            if text and len(text) > 1 and "ID：" not in text:
                                print(f"找到可能的账户名称元素: {text}")
                                driver.execute_script("arguments[0].click();", element)
                                print(f"已点击账户名称: {text}")
                                time.sleep(2)
                                
                                # 点击后尝试查找切换按钮
                                try:
                                    switch_button = WebDriverWait(driver, 3).until(
                                        EC.element_to_be_clickable((By.XPATH, "//span[@class='button-text' and text()='切换']"))
                                    )
                                    driver.execute_script("arguments[0].click();", switch_button)
                                    print("已点击切换按钮")
                                    time.sleep(2)
                                    
                                    # 检查是否出现了账户列表
                                    account_elements = driver.find_elements(By.CSS_SELECTOR, "div.navigator-account")
                                    if account_elements and len(account_elements) > 0:
                                        print(f"成功打开账户列表，找到 {len(account_elements)} 个账户")
                                        break
                                except:
                                    # 如果找不到切换按钮，尝试下一个元素
                                    continue
                                
                                # 如果已经找到并点击了切换按钮，跳出循环
                                if account_elements and len(account_elements) > 0:
                                    break
                        except:
                            continue
                    
                    # 如果已经找到并点击了切换按钮，跳出循环
                    if account_elements and len(account_elements) > 0:
                        break
                except:
                    continue
    except Exception as e:
        print(f"尝试点击账户名称失败: {str(e)}")
    
    # 尝试使用click_switch_button函数
    if not account_elements or len(account_elements) == 0:
        print("\n尝试使用click_switch_button函数...")
        if click_switch_button(driver):
            print("成功使用click_switch_button函数点击切换按钮")
            time.sleep(2)
    
    # 循环处理账户列表中的账户
    while processed_count < max_accounts:
        try:
            # 等待账户列表加载
            print("等待账户列表加载...")
            
            # 尝试多种方式定位账户列表
            account_elements = None
            list_selectors = [
                "div.navigator-account",
                "div.oc-item-group",
                "div.ovui-infinite-scroll-item",
                "div.account-item"
            ]
            
            for selector in list_selectors:
                try:
                    elements = driver.find_elements(By.CSS_SELECTOR, selector)
                    if elements and len(elements) > 0:
                        print(f"通过选择器 '{selector}' 找到 {len(elements)} 个账户元素")
                        account_elements = elements
                        break
                except:
                    continue
            
            if not account_elements or len(account_elements) == 0:
                print("未能找到账户列表元素")
                # 尝试再次点击切换按钮
                if click_switch_button(driver):
                    print("已重新点击切换按钮")
                    time.sleep(2)
                    continue
                else:
                    print("无法找到账户列表，退出处理")
                    break
            
            print(f"找到 {len(account_elements)} 个账户")
            
            # 选择下一个未处理的账户
            next_account_index = -1
            for i in range(len(account_elements)):
                if i not in processed_indices:
                    next_account_index = i
                    break
            
            # 如果所有账户都已处理，选择第一个账户
            if next_account_index == -1:
                print("所有账户都已处理，选择第一个账户")
                next_account_index = 0
            
            print(f"选择账户，索引为 {next_account_index}")
            
            # 获取账户名称和ID用于记录
            try:
                account_element = account_elements[next_account_index]
                
                # 尝试获取账户名称
                try:
                    account_name_span = account_element.find_element(By.CSS_SELECTOR, "span.oc-typography-value-int")
                    account_name = account_name_span.text if account_name_span else "未知账户"
                except:
                    try:
                        # 备用方法获取账户名称
                        account_name = account_element.text.split('\n')[0] if account_element.text else "未知账户"
                    except:
                        account_name = f"账户_{next_account_index}"
                
                # 尝试获取账户ID
                try:
                    id_span = account_element.find_element(By.XPATH, ".//span[contains(text(), 'ID：')]")
                    account_id = id_span.text.replace("ID：", "") if id_span else ""
                except:
                    account_id = ""
                
                print(f"准备点击账户: {account_name} (ID: {account_id})")
            except Exception as e:
                print(f"获取账户信息失败: {str(e)}")
                account_name = f"账户_{next_account_index}"
                account_id = ""
            
            # 点击选择账户
            try:
                # 尝试多种方式点击账户
                try:
                    # 方法1: 使用JavaScript点击
                    driver.execute_script("arguments[0].click();", account_elements[next_account_index])
                    print(f"已使用JavaScript点击账户: {account_name}")
                except Exception as e:
                    print(f"JavaScript点击失败: {str(e)}")
                    try:
                        # 方法2: 直接点击
                        account_elements[next_account_index].click()
                        print(f"已直接点击账户: {account_name}")
                    except Exception as e:
                        print(f"直接点击失败: {str(e)}")
                        try:
                            # 方法3: 使用ActionChains点击
                            actions = ActionChains(driver)
                            actions.move_to_element(account_elements[next_account_index]).click().perform()
                            print(f"已使用ActionChains点击账户: {account_name}")
                        except Exception as e:
                            print(f"ActionChains点击失败: {str(e)}")
                            raise Exception("所有点击方法都失败")
                
                # 等待页面加载
                print("等待页面加载完成...")
                time.sleep(8)
                
                # 记录已处理的账户索引
                processed_indices.add(next_account_index)
                processed_count += 1
                
                if account_id:
                    processed_ids.add(account_id)
                if account_name:
                    processed_names.add(account_name)
                
                print(f"已完成账户 {account_name} 的处理")
                
                # 如果还需要处理更多账户，点击当前账户名称以显示切换按钮
                if processed_count < max_accounts:
                    # 尝试多种方法打开账户列表
                    if not click_switch_button(driver):
                        print("使用click_switch_button函数失败，尝试其他方法")
                        
                        try:
                            print("\n尝试点击当前账户名称...")
                            # 尝试多种方式定位账户名称
                            for selector in name_selectors:
                                try:
                                    if selector.startswith("//"):
                                        elements = driver.find_elements(By.XPATH, selector)
                                    else:
                                        elements = driver.find_elements(By.CSS_SELECTOR, selector)
                                    
                                    for element in elements:
                                        try:
                                            text = element.text
                                            if text and len(text) > 1 and "ID：" not in text:
                                                driver.execute_script("arguments[0].click();", element)
                                                print(f"已点击账户名称: {text}")
                                                time.sleep(2)
                                                
                                                # 点击后尝试查找切换按钮
                                                try:
                                                    switch_button = WebDriverWait(driver, 3).until(
                                                        EC.element_to_be_clickable((By.XPATH, "//span[@class='button-text' and text()='切换']"))
                                                    )
                                                    driver.execute_script("arguments[0].click();", switch_button)
                                                    print("已点击切换按钮")
                                                    time.sleep(2)
                                                    break
                                                except:
                                                    continue
                                        except:
                                            continue
                                    
                                    # 检查是否出现了账户列表
                                    account_elements_check = driver.find_elements(By.CSS_SELECTOR, "div.navigator-account")
                                    if account_elements_check and len(account_elements_check) > 0:
                                        print("成功打开账户列表")
                                        break
                                except:
                                    continue
                        except Exception as e:
                            print(f"尝试点击账户名称失败: {str(e)}")
                            break
            except Exception as e:
                print(f"点击账户失败: {str(e)}")
                break
        except Exception as e:
            print(f"处理账户时出错: {str(e)}")
            break
    
    print(f"\n总共成功处理了 {processed_count} 个账户")
    
    # 显示已处理的账户ID和名称
    if processed_ids:
        print("\n已处理的账户ID:")
        for i, account_id in enumerate(processed_ids):
            print(f"{i+1}. {account_id}")
    
    if processed_names:
        print("\n已处理的账户名称:")
        for i, account_name in enumerate(processed_names):
            print(f"{i+1}. {account_name}")
    
    return processed_count

def login_account_console(max_accounts=None, max_retries=3):
    """
    登录业务后台并处理账户数据
    
    Args:
        max_accounts: 最大处理账户数量，如果为None则处理所有可用账户
        max_retries: 切换账户失败时的最大重试次数
    """
    # 设置Chrome选项
    options = webdriver.ChromeOptions()
    # options.add_argument('--headless')  # 无头模式，取消注释可以在后台运行
    options.add_argument('--disable-gpu')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')
    
    # 初始化Chrome浏览器，不使用ChromeDriverManager
    driver = webdriver.Chrome(options=options)
    
    try:
        # 打开业务后台
        driver.get(require_env("ACCOUNT_LOGIN_URL", ACCOUNT_LOGIN_URL))
        driver.maximize_window()
        print("已打开业务后台页面")
        
        # 等待并点击"立即登录"按钮
        login_button = WebDriverWait(driver, 10).until(
            EC.element_to_be_clickable((By.CLASS_NAME, "src-pages-Login-components-LoginCard-index-module__to-login--Z0fdv--212e2"))
        )
        login_button.click()
        print("已点击'立即登录'按钮")
        
        # 等待并点击"密码登录"
        password_login = WebDriverWait(driver, 10).until(
            EC.element_to_be_clickable((By.CLASS_NAME, "src-pages-Login-components-Phone-index-module__login-tip--BV02_--212e2"))
        )
        password_login.click()
        print("已切换到'密码登录'")
        
        # 输入手机号
        phone_input = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "input.life-core-input.life-core-input-size-md[placeholder='手机号码']"))
        )
        phone_input.clear()
        phone_input.send_keys(require_env("ACCOUNT_LOGIN_PHONE", ACCOUNT_LOGIN_PHONE))
        print("已输入手机号")
        
        # 输入密码
        password_input = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "input.life-core-input.life-core-input-size-md[placeholder='密码']"))
        )
        password_input.clear()
        password_input.send_keys(require_env("ACCOUNT_LOGIN_PASSWORD", ACCOUNT_LOGIN_PASSWORD))
        print("已输入密码")
        
        # 勾选同意协议复选框
        checkbox = WebDriverWait(driver, 10).until(
            EC.element_to_be_clickable((By.CSS_SELECTOR, "span.life-core-checkbox-icon"))
        )
        checkbox.click()
        print("已勾选同意协议")
        
        # 点击登录按钮
        submit_button = WebDriverWait(driver, 10).until(
            EC.element_to_be_clickable((By.CSS_SELECTOR, "button.life-core-btn.life-core-btn-size-md.life-core-btn-type-primary"))
        )
        submit_button.click()
        print("已点击登录按钮")
        
        # 等待登录成功，出现选择公司页面
        print("等待登录完成，准备选择公司...")
        # 使用显式等待而不是固定时间等待
        try:
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.XPATH, "//div[contains(@class, 'box-account-item')]"))
            )
        except TimeoutException:
            print("等待选择公司页面超时，可能已经自动登录")
        
        # 选择第一个公司（巴克游艇海钓俱乐部）
        # 使用XPath选择第一个公司项
        try:
            first_company = WebDriverWait(driver, 5).until(
                EC.element_to_be_clickable((By.XPATH, "//div[contains(@class, 'box-account-item')]"))
            )
            first_company.click()
            print("已选择第一个公司")
            
            # 点击确认登录按钮
            confirm_button = WebDriverWait(driver, 5).until(
                EC.element_to_be_clickable((By.XPATH, "//button[contains(@class, 'life-core-btn-type-primary') and contains(@class, 'src-pages-Login-components-LoginFlowCard-index-module__confirm')]"))
            )
            confirm_button.click()
            print("已点击确认登录按钮")
        except TimeoutException:
            print("未找到选择公司页面，可能已经自动登录")
        
        # 等待完全登录成功 - 减少固定等待时间
        print("等待完全登录成功...")
        time.sleep(3)  # 减少等待时间
        
        # 检查是否登录成功
        if driver.current_url:
            print("登录成功！")
            
            # 使用用户提供的具体类名定位"营销推广"菜单
            try:
                # 使用精确的类名定位"营销推广"
                marketing_menu = WebDriverWait(driver, 10).until(
                    EC.element_to_be_clickable((By.CSS_SELECTOR, "span.src-components-AsideMenu-index-module__label--Ncc3E--212e2"))
                )
                # 确保点击的是"营销推广"文本的元素
                if marketing_menu.text == "营销推广":
                    marketing_menu.click()
                    print("已点击'营销推广'菜单")
                else:
                    # 如果第一个找到的元素不是"营销推广"，则寻找所有具有相同类的元素
                    marketing_menus = driver.find_elements(By.CSS_SELECTOR, "span.src-components-AsideMenu-index-module__label--Ncc3E--212e2")
                    for menu in marketing_menus:
                        if menu.text == "营销推广":
                            menu.click()
                            print("已点击'营销推广'菜单")
                            break
            except Exception as e:
                print(f"尝试点击'营销推广'失败: {str(e)}")
            
            # 等待子菜单加载 - 使用更短的等待时间
            time.sleep(1)
            
            # 使用用户提供的具体类名定位"本地推"选项
            try:
                # 使用精确的类名和data-path属性定位"本地推"
                local_promo = WebDriverWait(driver, 10).until(
                    EC.element_to_be_clickable((By.CSS_SELECTOR, "span.navi_ad.src-components-AsideMenu-index-module__label--Ncc3E--212e2[data-path='https://localads.chengzijianzhan.cn/']"))
                )
                local_promo.click()
                print("已点击'本地推'选项")
            except Exception as e:
                print(f"尝试通过精确类名点击'本地推'失败: {str(e)}")
                try:
                    # 备选方法：如果无法通过精确类名定位，尝试通过文本内容定位
                    local_promo_elements = driver.find_elements(By.CSS_SELECTOR, "span.src-components-AsideMenu-index-module__label--Ncc3E--212e2")
                    for element in local_promo_elements:
                        if element.text == "本地推":
                            element.click()
                            print("已通过文本内容点击'本地推'选项")
                            break
                except Exception as e:
                    print(f"备选方法点击'本地推'失败: {str(e)}")
            
            # 等待"选择广告账户"对话框出现 - 使用显式等待
            print("等待'选择广告账户'对话框出现...")
            try:
                # 检查是否出现选择广告账户对话框
                account_dialog = WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.CLASS_NAME, "src-components-PoiSelector-index-module__outer--Kbnsz--212e2"))
                )
                print("检测到'选择广告账户'对话框")
                
                # 选择带有"已开户"标签的账户
                try:
                    # 定位带有"已开户"标签的账户按钮
                    account_with_tag = WebDriverWait(driver, 5).until(
                        EC.element_to_be_clickable((By.XPATH, "//button[contains(@class, 'src-components-PoiSelector-index-module__item--qHGlJ--212e2')]//span[contains(@class, 'life-core-tag') and contains(text(), '已开户')]/ancestor::button"))
                    )
                    account_with_tag.click()
                    print("已选择带有'已开户'标签的账户")
                except Exception as e:
                    print(f"尝试选择带有'已开户'标签的账户失败: {str(e)}")
                    # 备选方法：选择第一个可用的账户
                    try:
                        first_account = WebDriverWait(driver, 3).until(
                            EC.element_to_be_clickable((By.CSS_SELECTOR, "button.src-components-PoiSelector-index-module__item--qHGlJ--212e2"))
                        )
                        first_account.click()
                        print("已选择第一个可用的账户")
                    except Exception as e:
                        print(f"尝试选择第一个可用的账户失败: {str(e)}")
                
                # 等待"确认前往"按钮变为可点击状态 - 减少等待时间
                time.sleep(1)
                
                # 点击"确认前往"按钮
                try:
                    # 首先检查按钮是否被禁用
                    confirm_buttons = driver.find_elements(By.XPATH, "//div[contains(@class, 'src-components-PoiSelector-index-module__footer')]//button[contains(text(), '确认前往')]")
                    if confirm_buttons:
                        # 修复类型检查警告
                        button_class = confirm_buttons[0].get_attribute('class') or ""
                        if 'life-core-btn-disabled' not in button_class:
                            confirm_buttons[0].click()
                            print("已点击'确认前往'按钮")
                        else:
                            print("'确认前往'按钮当前被禁用，尝试先选择账户...")
                            # 如果按钮被禁用，可能需要先选择账户
                            account_buttons = driver.find_elements(By.CSS_SELECTOR, "button.src-components-PoiSelector-index-module__item--qHGlJ--212e2")
                            if account_buttons:
                                account_buttons[0].click()
                                print("已重新选择账户")
                                time.sleep(1)
                                # 再次尝试点击确认按钮
                                confirm_buttons = driver.find_elements(By.XPATH, "//div[contains(@class, 'src-components-PoiSelector-index-module__footer')]//button[contains(text(), '确认前往')]")
                                if confirm_buttons:
                                    button_class = confirm_buttons[0].get_attribute('class') or ""
                                    if 'life-core-btn-disabled' not in button_class:
                                        confirm_buttons[0].click()
                                        print("已点击'确认前往'按钮")
                                    else:
                                        print("'确认前往'按钮仍然被禁用，无法点击")
                except Exception as e:
                    print(f"尝试点击'确认前往'按钮失败: {str(e)}")
                
            except TimeoutException:
                print("未检测到'选择广告账户'对话框，可能已经跳过")
            
            # 等待页面加载完成 - 使用智能等待
            print("等待页面加载完成...")
            
            # 使用轮询方式检查页面是否加载完成，最多等待30秒
            start_time = time.time()
            page_loaded = False
            max_wait_time = 30  # 最长等待30秒
            
            while time.time() - start_time < max_wait_time and not page_loaded:
                # 检查是否有"立即推广"按钮出现，表示页面已加载
                try:
                    # 使用短超时快速检查
                    promote_span = WebDriverWait(driver, 1).until(
                        EC.presence_of_element_located((By.XPATH, "//span[@class='button-text' and contains(text(), '立即推广')]"))
                    )
                    page_loaded = True
                    print(f"页面加载完成，用时 {time.time() - start_time:.1f} 秒")
                except TimeoutException:
                    # 检查页面加载状态
                    page_state = driver.execute_script('return document.readyState;')
                    if page_state == 'complete':
                        # 如果页面已完成加载但没有找到按钮，再等待一小段时间
                        time.sleep(1)
                    print(f"等待页面加载中... ({time.time() - start_time:.1f}秒)")
            
            if not page_loaded:
                print(f"页面加载超时，已等待 {max_wait_time} 秒")
            
            # 处理弹窗中的"立即推广"按钮
            try:
                # 直接使用span标签定位"立即推广"按钮
                try:
                    promote_span = WebDriverWait(driver, 10).until(
                        EC.element_to_be_clickable((By.XPATH, "//span[@class='button-text' and contains(text(), '立即推广')]"))
                    )
                    # 点击包含该span的父按钮
                    driver.execute_script("arguments[0].closest('button').click();", promote_span)
                    print("已通过span标签定位并点击'立即推广'按钮")
                except Exception as e:
                    print(f"通过span标签定位'立即推广'按钮失败: {str(e)}")
                
                # 等待点击"立即推广"按钮后的页面加载
                print("等待点击'立即推广'按钮后的页面加载...")
                time.sleep(3)  # 减少等待时间
                
            except Exception as e:
                print(f"处理弹窗中的'立即推广'按钮失败: {str(e)}")
            
            # 开始处理所有账户，使用更新后的process_all_accounts函数
            print("\n开始处理所有账户...")
            process_all_accounts(driver, max_accounts)
            
        else:
            print("登录可能未成功，请检查页面状态")
        
        # 保持浏览器打开一段时间以便查看结果
        time.sleep(15)  # 减少最终等待时间
        
    except Exception as e:
        print(f"发生错误: {str(e)}")
    
    finally:
        # 关闭浏览器
        driver.quit()
        print("浏览器已关闭")

def click_account_in_list(driver, account_index):
    """
    在账户列表中点击指定索引的账户
    
    Args:
        driver: WebDriver实例
        account_index: 要点击的账户索引
        
    Returns:
        bool: 是否成功点击账户
    """
    try:
        # 查找所有账户元素
        account_elements = driver.find_elements(By.CSS_SELECTOR, "div.oc-item-group")
        
        if not account_elements or account_index >= len(account_elements):
            print(f"账户列表中没有索引为 {account_index} 的元素")
            
            # 尝试其他选择器
            selectors = [
                "div.account-item", 
                "div.ovui-infinite-scroll-item",
                "div.oc-dropdown-menu-item",
                "div.account-card"
            ]
            
            for selector in selectors:
                try:
                    account_elements = driver.find_elements(By.CSS_SELECTOR, selector)
                    if account_elements and account_index < len(account_elements):
                        print(f"通过选择器 '{selector}' 找到账户元素")
                        break
                except:
                    continue
            
            if not account_elements or account_index >= len(account_elements):
                print("无法找到账户元素")
                return False
        
        # 获取要点击的账户元素
        account = account_elements[account_index]
        
        # 打印账户文本，便于调试
        try:
            account_text = account.text
            print(f"准备点击账户: {account_text}")
        except:
            print("无法获取账户文本")
        
        # 确保元素在视图中
        driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", account)
        time.sleep(1)
        
        # 尝试多种方式点击账户
        try:
            # 方法1: 使用JavaScript点击
            driver.execute_script("arguments[0].click();", account)
            print("已通过JavaScript点击账户")
            return True
        except Exception as e:
            print(f"通过JavaScript点击账户失败: {str(e)}")
            
            try:
                # 方法2: 直接点击
                account.click()
                print("已直接点击账户")
                return True
            except Exception as e:
                print(f"直接点击账户失败: {str(e)}")
                
                try:
                    # 方法3: 使用ActionChains点击
                    actions = ActionChains(driver)
                    actions.move_to_element(account).click().perform()
                    print("已通过ActionChains点击账户")
                    return True
                except Exception as e:
                    print(f"通过ActionChains点击账户失败: {str(e)}")
                    return False
    except Exception as e:
        print(f"点击账户列表中的账户失败: {str(e)}")
        return False

if __name__ == "__main__":
    # 可以在这里指定要处理的账户数量，如果不指定则处理所有可用账户
    login_account_console()

