export interface Translations {
  locale: string;
  translations: Record<string, string>;
  count: number;
}

export interface I18nOptions {
  locale?: string;
  prefix?: string;
  prefixes?: string[];
}

export async function fetchTranslations(options: I18nOptions = {}): Promise<Translations> {
  const { locale = 'en-us', prefix, prefixes } = options;
  
  const params = new URLSearchParams();
  params.set('locale', locale);
  
  if (prefix) {
    params.set('prefix', prefix);
  } else if (prefixes && prefixes.length > 0) {
    params.set('prefixes', prefixes.join(','));
  }
  
  const url = `/rest/i18n/translations?${params.toString()}`;
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });
  
  if (!response.ok) {
    throw new Error(`Failed to fetch translations: ${response.status} ${response.statusText}`);
  }
  
  return response.json();
}

export function createMessageFunction(translations: Record<string, string>) {
  return function message(key: string, args?: string[]): string {
    let msg = translations[key];
    
    if (!msg) {
      const atSignIndex = key.indexOf('@');
      if (atSignIndex >= 0 && key.length > atSignIndex + 1) {
        msg = key.substring(atSignIndex + 1);
      } else {
        return key;
      }
    }
    
    if (args && args.length > 0) {
      args.forEach((arg, index) => {
        msg = msg.replace(new RegExp(`\\{${index}\\}`, 'g'), arg);
      });
    }
    
    return msg;
  };
}
