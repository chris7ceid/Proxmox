import click
import Utils
import pandas as pd
import json
import warnings
from statsmodels.tools.sm_exceptions import ConvergenceWarning

warnings.filterwarnings("ignore", category=ConvergenceWarning)
warnings.filterwarnings("ignore", message="Non-stationary starting autoregressive parameters found")
warnings.filterwarnings("ignore", message="Non-invertible starting MA parameters found")


@click.group()
def main():
    """
    Auto Regressive Integrated Moving Average Model
    """
    pass


@main.command()
@click.option('-o', '--option', type=click.Choice(['check', 'predict'], case_sensitive=False), help="check: checks "
                                                                                                    "stationarity and"
                                                                                                    " plots acf pacf "
                                                                                                    "diagrams, "
                                                                                                    "predict: train, "
                                                                                                    "testing, "
                                                                                                    "predicting and "
                                                                                                    "ploting the "
                                                                                                    "results")
@click.option('-l', '--login', is_flag=True, default=False, help="Enable login mode")
@click.option('-db', '--dbscan', is_flag=True, default=False, help="Enable dbscan clustering")
@click.option('-p', '--pvalue', default=1, type=int, help='An integer value for p-value')
@click.option('-i', '--integ', default=0, type=int, help='An integer value for differencing-value')
@click.option('-q', '--qvalue', default=1, type=int, help='An integer value for q-value')
@click.option('-f', '--freq', type=click.Choice(['hour', 'day', 'week', 'month', 'year'], case_sensitive=False),
              default='week', help='Frequency option: choose from hour, day, week, month, or year (default: week)')
@click.option('-d', '--data', type=click.Choice(['memusedGB', 'cpu', 'loadavg'], case_sensitive=False),
              default='memusedGB', help='Frequency option: choose from hour, day, week, month, or year (default: week)')
def arma(option, login, dbscan, pvalue, integ, qvalue, freq, data):
    df_data = None
    if login:
        login_repsonse = Utils.signin_request()
        if login_repsonse.status_code == 200:
            login_data = login_repsonse.json()
            ticket = 'PVEAuthCookie=' + login_data["ticket"]
            # python main.py arma -o check -f day -d loadavg
            # python main.py arma -o predict -f day -d loadavg -p 7 -q 3
            # python main.py arma -o check -f week -d loadavg
            # python main.py arma -o predict -f week -d loadavg  -p 1 -q 3
            response = Utils.get_request(ticket, 'http://localhost:8080/api/node/resources'
                                                 '/node91/metrics?timeframe=' + freq)
            if response.status_code == 200:
                df_data = response.json()
            else:
                print(f"Failed to fetch metrics data. Status code: {response.status_code}")
        else:
            print(f"Failed to fetch login data. Status code: {login_repsonse.status_code}")
    else:
        with open('test_week_db_91.json') as file:
            df_data = json.load(file)

    if df_data:
        df = pd.DataFrame(df_data)
        df = df.dropna()
        df['memusedGB'] = df['memusedGB'].apply(
            lambda x: float(x.replace('GB', '').replace(',', '.')) if isinstance(x, str) else x)
        df['time'] = pd.to_datetime(df['time'])
        df['date_time'] = df['time']
        df.set_index('time', inplace=True)
        df.sort_index(inplace=True)
        df.asfreq(Utils.df_freq(freq))
        if option == 'check':
            Utils.plot_df(df, data)
        if dbscan:
            df = Utils.dbscan(df, data)
        if option == 'check':
            Utils.acf_pacf(df, data)
        elif option == 'predict':
            Utils.arma(df, pvalue, integ, qvalue, Utils.df_freq(freq), data)


if __name__ == '__main__':
    main()
