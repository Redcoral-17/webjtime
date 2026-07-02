import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '62b9b4808eb0bdfd31ee269596dd07605ae7ee6958c6c6d302f6a96878ce609f') {
    pending.push(import('./chunks/chunk-b17dcf31f0ed5d1099ccda9db06d742a7e6505935a324ab80f085e2ce30e80fa.js'));
  }
  if (key === 'f80f3c0800fcf4d5184662ce740c234bc3a8b760b671b374a6f06acee8da0e81') {
    pending.push(import('./chunks/chunk-4ca1007fd47abc4e1ceb01f45212eb96a519455f938f75a9dfdce8b3e8b0d978.js'));
  }
  if (key === 'b0de75eb03c44db1ea061afd3a10ef064ade77f806f45e989b2d1fa143be82c3') {
    pending.push(import('./chunks/chunk-b17dcf31f0ed5d1099ccda9db06d742a7e6505935a324ab80f085e2ce30e80fa.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}