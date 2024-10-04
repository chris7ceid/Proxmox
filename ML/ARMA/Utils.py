from sklearn.cluster import DBSCAN
import matplotlib.pyplot as plt
import pandas as pd
import matplotlib.dates as mdates
import statsmodels.api as sm
from sklearn.metrics import mean_squared_error, mean_absolute_error
from statsmodels.tsa.stattools import adfuller
from statsmodels.graphics.tsaplots import plot_acf, plot_pacf
import requests


def get_request(PVEAuthCookie, url):
    headers = {
        'Content-Type': 'application/json',
        'Cookie': f'{PVEAuthCookie}'
    }

    return requests.get(url, headers=headers)


def signin_request():
    url = "http://localhost:8080/api/auth/signin"
    payload = {
        "username": "testadmin@pve",
        "password": "Z75b1+tYK[bZ"
    }

    return requests.post(url, json=payload)


def dbscan(df: pd.DataFrame, data):
    dbscan = DBSCAN(eps=0.5, min_samples=2)

    data_values = df[[data]].values

    df['dbscan_labels'] = dbscan.fit_predict(data_values)

    df_cleaned = df[df['dbscan_labels'] != -1]

    plt.figure(figsize=(10, 6))
    plt.scatter(df[df['dbscan_labels'] == -1].index, df[df['dbscan_labels'] == -1][data], c='red',
                label='Outliers', marker='x')
    plt.scatter(df_cleaned.index, df_cleaned[data], c='green', label='Cleaned Data')
    plt.title('DBSCAN Outlier Detection on memusedGB')
    plt.xlabel('Index')
    plt.ylabel(data)
    plt.legend()
    plt.show()
    return df_cleaned


def plot_df(df: pd.DataFrame, data):

    plt.figure(figsize=(10, 6))
    plt.plot(df['date_time'], df[data], marker='o')
    plt.title(data + ' Over Time')
    plt.xlabel('Time')
    plt.ylabel(data)
    plt.gca().xaxis.set_major_formatter(mdates.DateFormatter('%Y-%m-%d %H:%M'))
    plt.xticks(rotation=45, fontsize=8)
    plt.tight_layout()
    plt.show()


def acf_pacf(df: pd.DataFrame, data):
    test_series = df[data]

    adf_test = adfuller(test_series.dropna())
    print(f'ADF Statistic: {adf_test[0]}')
    print(f'p-value: {adf_test[1]}')

    # ACF
    fig, ax = plt.subplots(figsize=(12, 6))
    plot_acf(test_series, lags=20, ax=ax)
    ax.set_title('ACF Plot')
    plt.show()

    # PACF
    fig, ax = plt.subplots(figsize=(12, 6))
    plot_pacf(test_series, lags=20, method='ywm', ax=ax)
    ax.set_title('PACF Plot')
    plt.show()


def arma(df: pd.DataFrame, pvalue, integ, qvalue, freq, data):
    mem_series = df[data]

    mem_series = mem_series.asfreq(freq)

    total_rows = len(mem_series)

    train_size = int(total_rows * 0.8)

    train_data = mem_series.iloc[:train_size]
    test_data = mem_series.iloc[train_size:]

    model = sm.tsa.ARIMA(train_data, order=(pvalue, integ, qvalue))
    model_fit = model.fit()

    start = len(train_data)
    end = len(mem_series) - 1
    predictions = model_fit.predict(start=start, end=end)

    predictions = pd.Series(predictions, index=test_data.index)

    mse = mean_squared_error(test_data, predictions)
    mae = mean_absolute_error(test_data, predictions)

    print(f"Mean Squared Error: {mse}")
    print(f"Mean Absolute Error: {mae}")

    plt.figure(figsize=(12, 6))
    plt.plot(mem_series.index, mem_series, label='Actual Values', color='blue')
    plt.plot(predictions.index, predictions, label='Predicted Values', color='red',
             linestyle='--')

    plt.title('ARIMA MODEL')
    plt.xlabel('Date')
    plt.ylabel(data)
    plt.legend()
    plt.grid(True)
    plt.show()


def df_freq(freq):
    if freq == "year":
        return '7d'
    elif freq == "month":
        return '12h'
    elif freq == "week":
        return '3h'
    elif freq == "day":
        return '30min'
    elif freq == "hour":
        return 'min'
