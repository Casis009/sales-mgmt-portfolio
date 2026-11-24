package jp.co.hikari.hanbai.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 登録・編集画面のテスト用の送信処理。入力画面を開いて二重送信防止のトークンを取得し、同じセッションで POST する。
 *
 * @param mvc MockMvc
 * @param operator 操作者
 */
public record FormSubmitter(MockMvc mvc, LoginUser operator) {

  /**
   * 画面を GET して本文を返す。
   *
   * @param request リクエスト
   * @return 本文
   * @throws Exception 実行に失敗した場合
   */
  public String page(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.with(user(operator))).andReturn().getResponse().getContentAsString();
  }

  /**
   * 入力画面を開いて送信する。値の「,」は複数値の区切り。
   *
   * @param formUrl 入力画面の URL
   * @param postUrl 送信先
   * @param params 送信する値
   * @return 送信結果
   * @throws Exception 実行に失敗した場合
   */
  public MvcResult submit(String formUrl, String postUrl, Map<String, String> params)
      throws Exception {
    MockHttpSession session = new MockHttpSession();
    MvcResult opened = mvc.perform(get(formUrl).session(session).with(user(operator))).andReturn();
    assertThat(opened.getResponse().getStatus()).as(formUrl).isEqualTo(200);
    String token =
        (String) opened.getModelAndView().getModel().get(TransactionTokenManager.PARAM);
    MockHttpServletRequestBuilder request =
        post(postUrl)
            .session(session)
            .with(csrf())
            .with(user(operator))
            .param(TransactionTokenManager.PARAM, token);
    params.forEach((k, v) -> request.param(k, v.split(",", -1)));
    return mvc.perform(request).andReturn();
  }

  /**
   * 送信して、再表示された画面の本文を返す（入力エラー・業務エラーの確認用）。
   *
   * @param formUrl 入力画面の URL
   * @param postUrl 送信先
   * @param params 送信する値
   * @return 本文（成功してリダイレクトした場合は空）
   * @throws Exception 実行に失敗した場合
   */
  public String submitAndRead(String formUrl, String postUrl, Map<String, String> params)
      throws Exception {
    return submit(formUrl, postUrl, params).getResponse().getContentAsString();
  }
}
